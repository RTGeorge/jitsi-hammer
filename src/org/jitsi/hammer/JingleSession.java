/*
 * Jitsi-Hammer, A traffic generator for Jitsi Videobridge.
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jitsi.hammer;


import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;
import org.ice4j.ice.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.hammer.utils.*;
import org.jitsi.hammer.extension.*;

import net.java.sip.communicator.impl.protocol.jabber.jinglesdp.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.ContentPacketExtension.*;
import net.java.sip.communicator.service.protocol.media.*;

import java.io.*;
import java.util.*;


/**
 * 
 * @author Thomas Kuntz
 *
 * <tt>JingleSession</tt> represent a Jingle,ICE and RTP/RTCP session with
 * jitsi-videobridge : it simulate a jitmeet user by setting up an 
 * ICE stream and then sending fake audio/video data using RTP
 * to the videobridge.
 *
 */
public class JingleSession implements PacketListener {
    /**
     * The XMPP server info to which this <tt>JingleSession</tt> will
     * communicate
     */
    private HostInfo serverInfo;
    
    /**
     * The username/nickname taken by this <tt>JingleSession</tt> in the
     * MUC chatroom
     */
    private String username;
    
    
    /**
     * The <tt>ConnectionConfiguration</tt> equivalent of <tt>serverInfo</tt>.
     */
    private ConnectionConfiguration config;
    
    /**
     * The object use to connect to and then communicate with the XMPP server.
     */
    private XMPPConnection connection;
    
    /**
     * The object use to connect to and then send message to the MUC chatroom.
     */
    private MultiUserChat muc;
    
    
    /**
     * The registry containing the dynamic payload types learned in the
     * session-initiate (to use back in the session-accept)
     */
    DynamicPayloadTypeRegistry ptRegistry = new DynamicPayloadTypeRegistry();
    
    /**
     * A Map mapping a media type (audio, video, data), with a list of format
     * that can be handle by libjitsi
     */
    Map<String,List<MediaFormat>> possibleFormatMap =
            new HashMap<String,List<MediaFormat>>();
    
    /**
     * A Map mapping a media type (audio, video, data), with a <tt>MediaFormat</tt>
     * representing the selected format for the stream handling this media type.
     * 
     * The MediaFormat in this Map has been chosen in <tt>possibleFormatMap</tt>
     */
    Map<String,MediaFormat> selectedFormat = new HashMap<String,MediaFormat>();
    
    /**
     * The IQ message received by the XMPP server to initiate the Jingle session.
     * 
     * It contains a list of <tt>ContentPacketExtension</tt> representing
     * the media and their formats the videobridge is offering to send/receive
     * and their corresponding transport information (IP, port, etc...).
     */
    private JingleIQ sessionInitiate;
    
    /**
     * The IQ message send by this <tt>JingleSession</tt> to the XMPP server
     * to accept the Jingle session.
     * 
     * It contains a list of <tt>ContentPacketExtension</tt> representing
     * the media and format, with their corresponding transport information,
     * that this <tt>JingleSession</tt> accept to receive and send. 
     */
    private JingleIQ sessionAccept;

    /**
     * A Map of the different <tt>MediaStream</tt> this <tt>JingleSession</tt>
     * handles.
     */
    private Map<String,MediaStream> mediaStreamMap;
    
    /**
     * The <tt>Agent</tt> handling the ICE protocol of the stream
     */
    private Agent agent;
    
    /**
     * <tt>Presence</tt> packet containing the SSRC of the streams of this
     * <tt>JingleSession</tt> (ns = http://estos.de/ns/mjs).
     */
    private Packet presencePacketWithSSRC;
    
    /**
     * Instantiates a <tt>JingleSession</tt> with a default username that
     * will connect to the XMPP server contained in <tt>hostInfo</tt>.
     *  
     * @param hostInfo the XMPP server informations needed for the connection.
     */
    public JingleSession(HostInfo hostInfo)
    {
        this(hostInfo,null);
    }
    
    /**
     * Instantiates a <tt>JingleSession</tt> with a specified <tt>username</tt>
     * that will connect to the XMPP server contained in <tt>hostInfo</tt>.
     * 
     * @param hostInfo the XMPP server informations needed for the connection.
     * @param username the username used by this <tt>JingleSession</tt> in the
     * connection.
     * 
     */
    public JingleSession(HostInfo hostInfo,String username)
    {
        this.serverInfo = hostInfo;
        this.username = (username == null) ? "Anonymous" : username;
        
        
        config = new ConnectionConfiguration(
                serverInfo.getXMPPHostname(),
                serverInfo.getPort(),
                serverInfo.getXMPPDomain());
        
        connection = new XMPPConnection(config);
        
        connection.addPacketListener(this,new PacketFilter()
            {
                public boolean accept(Packet packet)
                {
                    return (packet instanceof JingleIQ);
                }
            });
        
        //config.setDebuggerEnabled(true);
    }


    /**
     * Connect to the XMPP server then to the MUC chatroom.
     * @throws XMPPException if the connection to the XMPP server goes wrong
     */
    public void start() throws XMPPException
    {
        connection.connect();
        connection.loginAnonymously();

        
        String roomURL = serverInfo.getRoomName()+"@"+serverInfo.getMUCDomain();
        muc = new MultiUserChat(connection, roomURL);
        while(true)
        {
            try
            {
                muc.join(username);
            }
            catch (XMPPException e)
            {
                /*
                 * IF the nickname is already taken in the MUC (code 409)
                 * then we append '_' to the username, and retry
                 */
                if(e.getXMPPError().getCode() == 409)
                {
                    username=username+'_';
                    continue;
                }
                else throw e;
            }
            break;
        }
        muc.sendMessage("Hello World!");
        
        
        /*
         * Send a Presence packet containing a Nick extension so that the
         * nickname is correctly displayed in jitmeet
         */
        Packet presencePacket = new Presence(Presence.Type.available);
        presencePacket.setTo(roomURL + "/" + username);
        presencePacket.addExtension(new Nick(username));
        connection.sendPacket(presencePacket);
        
        
        /*
         * Add a simple message listener that will just display in the terminal
         * received message (and respond back with a "C'est pas faux");
         */
        //muc.addMessageListener(
        //       new MyPacketListener(muc,roomURL +"/" + muc.getNickname()) );
    }

    /**
     * Stop all media stream and disconnect from the MUC and the XMPP server
     */
    public void stop()
    {
        for(MediaStream stream : mediaStreamMap.values())
        {
            stream.stop();
        }
        
        connection.sendPacket(
                JinglePacketFactory.createSessionTerminate(
                        sessionAccept.getFrom(),
                        sessionAccept.getTo(),
                        sessionAccept.getSID(),
                        Reason.GONE,
                        "Bye Bye"));
        
        muc.leave();
        connection.disconnect();
    }
    
    
    /**
     * acceptJingleSession create a accept-session Jingle message and
     * send it to the initiator of the session.
     * The initiator is taken from the From attribute 
     * of the initiate-session message.
     */
    private void acceptJingleSession()
    {
        IceMediaStreamGenerator iceMediaStreamGenerator = null;
        List<MediaFormat> listFormat = null;
        Map<String,ContentPacketExtension> contentMap =
                new HashMap<String,ContentPacketExtension>();
        ContentPacketExtension content = null;
        
        for(ContentPacketExtension cpe : sessionInitiate.getContentList())
        {
            //data isn't correctly handle by libjitsi for now, so we handle it
            //differently than the other MediaType
            if(cpe.getName().equalsIgnoreCase("data"))
            {
                content = HammerUtils.createDescriptionForDataContent(
                        CreatorEnum.responder,
                        SendersEnum.both);
            }
            else
            {
                listFormat = JingleUtils.extractFormats(
                        cpe.getFirstChildOfType(RtpDescriptionPacketExtension.class),
                        ptRegistry);
                
                
                //extractRTPExtensions() TODO ?
                
                
                
                possibleFormatMap.put(
                        cpe.getName(),
                        listFormat);
                
                selectedFormat.put(
                        cpe.getName(),
                        HammerUtils.selectFormat(cpe.getName(),listFormat));
                
                
                content = JingleUtils.createDescription(
                                CreatorEnum.responder, 
                                cpe.getName(),
                                SendersEnum.both,
                                listFormat,
                                null,
                                ptRegistry,
                                null);
            }
            
            contentMap.put(cpe.getName(),content);
        }
        //We remove the content for the data (because data is not handle
        //for now by libjitsi
        //FIXME
        contentMap.remove("data");
        
        
        
        iceMediaStreamGenerator = IceMediaStreamGenerator.getInstance();
        try
        {
            agent = iceMediaStreamGenerator.generateIceMediaStream(
                    contentMap.keySet(),
                    null,
                    null);
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
        
        HammerUtils.addRemoteCandidateToAgent(
                agent,
                sessionInitiate.getContentList());
        HammerUtils.addLocalCandidateToContentList(
                agent,
                contentMap.values());
        
        
        
        
        //create mediastream
        mediaStreamMap = HammerUtils.generateMediaStream(
                selectedFormat,
                ptRegistry);
        
        
        
        /*
         * Send the SSRC of the different media in a "media" tag
         * It's not necessary but its a copy of Jitsi Meet behavior
         * 
         * Also, without sending this packet, there are error logged
         *  in the javascript console of the Jitsi Meet initiator :
         * "No video type for ssrc: 13365845"
         * It seems like Jitsi Meet can work arround this error,
         * but better safe than sorry.
         */
        presencePacketWithSSRC = new Presence(Presence.Type.available);
        String recipient = 
                serverInfo.getRoomName()
                +"@"
                +serverInfo.getMUCDomain()
                + "/"
                + username;
        presencePacketWithSSRC.setTo(recipient);
        presencePacketWithSSRC.addExtension(new Nick(username));
        MediaPacketExtension mediaPacket = new MediaPacketExtension();
        for(String key : mediaStreamMap.keySet())
        {
            String str = String.valueOf(mediaStreamMap.get(key).getLocalSourceID());
            mediaPacket.addSource(
                    key,
                    str,
                    MediaDirection.SENDRECV.toString());
        }
        presencePacketWithSSRC.addExtension(mediaPacket);
        //connection.sendPacket(presencePacketWithSSRC);

        
        
        
        //Creation of a session-accept message
        sessionAccept = JinglePacketFactory.createSessionAccept(
                sessionInitiate.getTo(),
                sessionInitiate.getFrom(),
                sessionInitiate.getSID(),
                contentMap.values());
        sessionAccept.setInitiator(sessionInitiate.getFrom());
        
        
        HammerUtils.addSSRCToContent(contentMap, mediaStreamMap);
        
        
        
        //Set fingerprint
        HammerUtils.setDtlsEncryptionOnTransport(
                mediaStreamMap,
                sessionAccept.getContentList(),
                sessionInitiate.getContentList());
        
        
        //Sending of the session-accept IQ
        connection.sendPacket(sessionAccept);
        System.out.println("Jingle accept-session message sent");
        
        
        agent.startConnectivityEstablishment();
        while(IceProcessingState.TERMINATED != agent.getState())
        {
            System.out.println("Connectivity Establishment in process");
            try
            {
                Thread.sleep(1500);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        
        
        //Add socket to the MediaStream
        HammerUtils.addSocketToMediaStream(agent, mediaStreamMap);
        
        
        
        //For now the DTLS is not started because there is a bug
        //that made the handshake fail
        for(MediaStream stream : mediaStreamMap.values())
        {
            SrtpControl control = stream.getSrtpControl();
            MediaType type = stream.getFormat().getMediaType();
            control.start(type);
        }
        
        
        for(MediaStream stream : mediaStreamMap.values())
        {
            stream.start();
        }
    }
    
    
  
    /**
     * Callback function used when a JingleIQ is received by the XMPP connector.
     * @param packet the packet received by the <tt>JingleSession</tt> 
     */
    public void processPacket(Packet packet)
    {
        JingleIQ jiq = (JingleIQ)packet;
        System.out.println("Jingle initiate-session message received");
        ackJingleIQ(jiq);
        switch(jiq.getAction())
        {
            case SESSION_INITIATE:
                sessionInitiate = jiq;
                acceptJingleSession();
                break;
            case ADDSOURCE:
                break;
            case REMOVESOURCE:
                break;
            default:
                System.out.println("Unknown Jingle IQ");
                break;
        }
    }
    
    
    /**
     * This function simply create an ACK packet to acknowledge the Jingle IQ
     * packet <tt>packetToAck</tt>.
     * @param packetToAck the <tt>JingleIQ</tt> that need to be acknowledge.
     */
    private void ackJingleIQ(JingleIQ packetToAck)
    {
        IQ ackPacket = IQ.createResultIQ(packetToAck);
        connection.sendPacket(ackPacket);
        System.out.println("Ack sent for JingleIQ");
    }
}
