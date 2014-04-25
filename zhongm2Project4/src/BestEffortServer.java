import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import spaceWar.Constants;
import spaceWar.SpaceCraft;


/**
 *  Class to receive and forward UDP packets containing
 *  updates from clients. In addition, it checks for 
 *  collisions caused by client movements and sends
 *  appropriate removal information.
 *  
 * @author bachmaer
 */
class BestEffortServer extends Thread {
	
	// Socket through which all client UDP messages
	// are received
	protected DatagramSocket gamePlaySocket = null;
	
	// Reference to the server which holds the sector to be updated
	SpaceGameServer spaceGameServer;

	DataInputStream dis = null;
	DataOutputStream dos = null;
	
	protected byte ipBytes[] = new byte[4];
	protected int port, code, x, y, heading;
	protected InetSocketAddress id;
	protected DatagramPacket pack = new DatagramPacket(new byte[24],24);
	
	
	/**
	 * Creates DatagramSocket through which all client update messages
	 * will be received and forwarded.
	 */
	public BestEffortServer(SpaceGameServer spaceGameServer) {
		
		// Save reference to the server
		this.spaceGameServer = spaceGameServer;
		
		try {

			gamePlaySocket = new DatagramSocket( Constants.SERVER_PORT );
			
		} catch (IOException e) {

			System.err.println("Error creating socket to receive and forward UDP messages.");
			spaceGameServer.playing = false;
		}
		
	} // end gamePlayServer
	
	
	/**
	 * Receiving update message and update sector.
	 */
	public void run() {

		// Receive and forward messages. Update the sector display
		while (spaceGameServer.playing){
			receivedReadAndForwardMessage();
			updateDisplay();
		}
		//TODO
	} // end run
	
	
	/**
	 * Method is used to receive and forward message.
	 * 
	 */
	public void receivedReadAndForwardMessage() 
	{
		
		try
		{
			gamePlaySocket.receive(pack);
			
			ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
			DataInputStream dis = new DataInputStream(bais);
			
			dis.read(ipBytes);
			port = dis.readInt();
			code = dis.readInt();
			x = dis.readInt();
			y = dis.readInt();
			heading = dis.readInt();
			id = new InetSocketAddress(InetAddress.getByAddress(ipBytes),port);


			spaceGameServer.selectiveForward(pack, (InetSocketAddress) pack.getSocketAddress(), gamePlaySocket);
			
		} catch (IOException e) {
			System.out.println("receivedReadAndForwardMessage()");
		}
	}
	
	//TODO
	
	/**
	 * Method is used to update the display of server sector.
	 * 
	 */
	protected void updateDisplay()
	{

		if(code == Constants.JOIN || code == Constants.UPDATE_SHIP);
		SpaceCraft ship = new SpaceCraft(id,x,y,heading);
        spaceGameServer.sector.updateOrAddSpaceCraft(id,x,y,heading);
      
		ArrayList<SpaceCraft> destroyed = spaceGameServer.sector.collisionCheck(ship);

		if(destroyed != null)
		{
			for(SpaceCraft sc : destroyed)
			{
				spaceGameServer.sendRemoves(sc);
			}
		}
	}
} // end BestEffortServer class
