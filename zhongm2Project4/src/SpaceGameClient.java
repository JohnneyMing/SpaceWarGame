import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import spaceWar.*;

/**
 * @author bachmaer, mingwei
 *
 * Driver class for a simple networked space game. Opponents try to destroy each 
 * other by ramming. Head on collisions destroy both ships. Ships move and turn 
 * through GUI mouse clicks. All friendly and alien ships are displayed on a 2D 
 * interface.  
 */
public class SpaceGameClient implements SpaceGUIInterface
{
	// Keeps track of the game state
	Sector sector;
	
	// User interface
	SpaceGameGUI gui;
	
	// IP address and port to identify ownship and the 
	// DatagramSocket being used for game play messages.
	InetSocketAddress ownShipID;
	
	// Socket for sending and receiving
	// game play messages.
	DatagramSocket gamePlaySocket;

	// Socket used to register and to receive remove information
	// for ships and 
	Socket reliableSocket;
	
	// Set to false to stops all receiving loops
	boolean playing = true;
	
	static final boolean DEBUG = false;
	
	DataInputStream dis;
	DataOutputStream dos;
	
	/**
	 * Creates all components needed to start a space game. Creates Sector 
	 * canvas, GUI interface, a Sender object for sending update messages, a 
	 * Receiver object for receiving messages.
	 * @throws UnknownHostException 
	 */
	public SpaceGameClient()
	{
		// Create UDP Datagram Socket for sending and receiving
		// game play messages.
		try {
			
			gamePlaySocket = new DatagramSocket();
			gamePlaySocket.setSoTimeout(100);
			
			// Instantiate ownShipID using the DatagramSocket port
			// and the local IP address.
			ownShipID = new InetSocketAddress(InetAddress.getLocalHost(),gamePlaySocket.getLocalPort());
			
			// Create display, ownPort is used to uniquely identify the 
			// controlled entity.
			sector = new Sector( ownShipID );
			
			//	gui will call SpaceGame methods to handle user events
			gui = new SpaceGameGUI( this, sector ); 
			
			// Establish TCP connection with the server and pass the 
			// IP address and port number of the gamePlaySocket to the 
			// server.
			// TODO
			// Call a method that uses TCP/IP to receive obstacles 
			// from the server. 
			// TODO
			registerToServer();
		    
			// Start thread to listen on the TCP Socket and receive remove messages.
			// TODO
			new TCPThread().start();

			// Infinite loop or separate thread to receive update 
			// messages from the server and use the messages to 
			// update the sector display
			// TODO
			new UDPThread().start();

		} catch (SocketException e) {
			System.err.println("Error creating game play datagram socket.");
			System.err.println("Server is not opening.");

		} catch (UnknownHostException e) {
			System.err.println("Error creating ownship ID. Exiting.");
			System.err.println("Server is not opening.");
		}
		

	} // end SpaceGame constructor

	
	/**
	 * This Thread is used to listen on the TCP Socket and receive remove messages.
	 * 
	 * 
	 * @author Ming
	 *
	 */
	protected class TCPThread extends Thread {
		
		 DatagramPacket pack = new DatagramPacket(new byte[24],24);
		 protected byte ip [] = new byte[4];
		 protected int port, code, x, y, heading;
		 protected InetSocketAddress id;
		 
		 public void run()
		 {
			 while(playing)
			 {
				 receiveRemovemessage();
				 updateDisplay();			 
				 
			 }
		 }

		 /**
		  * Keep updating the sector after receiving remove message.
		  * 
		  */
		 private void updateDisplay()
		 {
			 if(code == Constants.REMOVE_TORPEDO)
			 {
				 Torpedo trop = new Torpedo(id,x,y,heading);
				 sector.removeTorpedo(trop);
			 }
			 else if (code == Constants.REMOVE_SHIP)
			 {
				 SpaceCraft sc = new SpaceCraft(id);
				 sector.removeSpaceCraft(sc);
			 }
		 }
		 
		 /**
		  * Receiving the remove message
		  * 
		  */
		 private void receiveRemovemessage()
		 {
			 try
			 {
				dis.read(ip);
				port = dis.readInt();
				code = dis.readInt();
				id = new InetSocketAddress(InetAddress.getByAddress(ip),port);

			} catch (IOException e)
			{} 
		 }
	}

	/***********************************************************************************************************************************/
	
	/**
	 * Thread to receive update messages from the server and use the messages to 
	 * update the sector display
	 * 
	 * @author Ming
	 *
	 */
	protected class UDPThread extends Thread
	{
		
		DatagramPacket pack = new DatagramPacket(new byte[24],24);
		protected byte ip[] = new byte[4];
		protected int port, code, x, y, heading;
		protected InetSocketAddress id;
		
		public void run()
		{
			  while(playing)
			  {
				receiveUpdateMessage();
			  }
		}
		
		/**
		 * This method is used to update the sector display by receiving UDP packet
		 * 
		 * 
		 */
		public void updateDisplay()
		{

			if(code == Constants.UPDATE_TORPEDO)
			{
				Torpedo torpedo = new Torpedo (id, x, y, heading);
				sector.updateOrAddTorpedo(torpedo);
			}
	        
			else
			{   
				AlienSpaceCraft s = new AlienSpaceCraft(id,x,y,heading);
				sector.updateOrAddSpaceCraft(s);
			}
		}
		
		/**
		 * Receiving update message.
		 * 
		 */
		public void receiveUpdateMessage()
		{
			try {
				gamePlaySocket.receive(pack);
				ByteArrayInputStream bais = new ByteArrayInputStream(pack.getData());
				DataInputStream dis = new DataInputStream(bais);
				
				dis.read(ip);
				port = dis.readInt();
				code = dis.readInt();
				x = dis.readInt();
				y = dis.readInt();
				heading = dis.readInt();
				id = new InetSocketAddress(InetAddress.getByAddress(ip),port);
				updateDisplay();
			} catch(SocketTimeoutException e){
				
			} 
			catch (IOException e) {
				e.printStackTrace();
				System.out.println("receiveUpdateMessage()");
			}	
		}
	}
	
	
	/***********************************************************************************************************************************/
	
	
	
	/**
	 * Establish TCP connection with server.
	 * 
	 */
	public void registerToServer()
	{
		
		try {
			
		    reliableSocket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT);	
			dos = new DataOutputStream(reliableSocket.getOutputStream());
			//Send register code
			dos.writeInt(Constants.REGISTER);
			//Send port number
			dos.writeInt(gamePlaySocket.getLocalPort());
			dis = new DataInputStream(reliableSocket.getInputStream());
		
			int flag = 0;
			do
			{
				flag = dis.readInt();
				
				if(flag != -1)
				{
					int y = dis.readInt();
					sector.addObstacle(flag, y);
				}
				
			}while(flag != -1);
			
			
		} catch (IOException e) {
			System.out.println("RegisterToServer()");
		}
	}
	

	/**
	 * Turn right.
	 */
	public void turnRight()
	{
		if (sector.ownShip != null) {
			
			if ( DEBUG ) System.out.println( " Right Turn " );
			
			// Update the display			
			sector.ownShip.rightTurn();
			
			// Send update message to server with new heading.
			// TODO
			doUpdateShip(Constants.UPDATE_SHIP);
		} 
		
	} // end turnRight


	/**
	 * Causes sector.ownShip to turn and sends an update message for the heading 
	 * change.
	 */
	public void turnLeft()
	{
		// See if the player has a ship in play
		if (sector.ownShip != null) {		
			
			if ( DEBUG ) System.out.println( " Left Turn " );
			
			// Update the display
			sector.ownShip.leftTurn();
			
			// Send update message to other server with new heading.
			// TODO
			doUpdateShip(Constants.UPDATE_SHIP);
		}		
		
	} // end turnLeft
	
	
	/**
	 * Causes sector.ownShip to turn and sends an update message for the heading 
	 * change.
	 */
	public void fireTorpedo()
	{
		// See if the player has a ship in play
		if (sector.ownShip != null) {		
			
			if ( DEBUG ) System.out.println( "Informing server of new torpedo" );
	
			// Send code to let server know a torpedo is being fired.
			// TODO
			// Send Position and heading
			// TODO

		    try
		    {
		    	Socket s = new Socket(Constants.SERVER_IP,Constants.SERVER_PORT);
		    	DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		    	dos.writeInt(Constants.FIRED_TORPEDO);
		    	int port = gamePlaySocket.getLocalPort();
		    	dos.writeInt(port);
		    	dos.writeInt(sector.ownShip.getXPosition());
		    	dos.writeInt(sector.ownShip.getYPosition());
		    	dos.writeInt(sector.ownShip.getHeading());

		    }catch(IOException e)
		    {
		    	System.out.println("fireTorpedo()");
		    }
		}		
		
	} // end turnLeft

	
	/**
	 * Causes sector.ownShip to move forward and sends an update message for the 
	 * position change. If there is an obstacle in front of
	 * the ship it will not move forward and a message is not sent. 
	 */
	public void moveFoward()
	{
		// Check if the player has and unblocked ship in the game
		if ( sector.ownShip != null && sector.clearInfront() ) {
			
			if ( DEBUG ) System.out.println( " Move Forward" );
			
			//Update the displayed position of the ship
			sector.ownShip.moveForward();
			
			// Send a message with the updated position to server
			// TODO	
			doUpdateShip(Constants.UPDATE_SHIP);
		}
								
	} // end moveFoward
	
	
	/**
	 * Causes sector.ownShip to move forward and sends an update message for the 
	 * position change. If there is an obstacle in front of
	 * the ship it will not move forward and a message is not sent. 
	 */
	public void moveBackward()
	{
		// Check if the player has and unblocked ship in the game
		if ( sector.ownShip != null && sector.clearBehind() ) {
			
			if ( DEBUG ) System.out.println( " Move Backward" );
			
			//Update the displayed position of the ship
			sector.ownShip.moveBackward();
			
			// Send a message with the updated position to server
			// TODO	
			
			doUpdateShip(Constants.UPDATE_SHIP);
		}
								
	} // end moveFoward


	/**
	 * Creates a new sector.ownShip if one does not exist. Sends a join message 
	 * for the new ship.
	 *
	 */
	public void join()
	{
		if (sector.ownShip == null )
		{

			if ( DEBUG ) System.out.println( " Join " );
			
			// Add a new ownShip to the sector display
			sector.createOwnSpaceCraft();
			
			// Send message to server let them know you have joined the game using the 
			// send object
			// TODO
			doUpdateShip(Constants.JOIN);
		}
		
	} // end join

	
	/**
	 *  Perform clean-up for application shut down
	 */
	public void stop()
	{
		if ( DEBUG ) System.out.println("stop");
		
		// Stop all thread and close all streams and sockets
		playing = false;
		
		// Send exit code to the server
		// TODO
		
		try{
			 Socket socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT);
			 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			 dos.writeInt(Constants.EXIT);
			 dos.writeInt(gamePlaySocket.getLocalPort());
			 socket.close();
			 dos.close();

		} catch (IOException e) {}
	} // end stop
	
	
	
	/**
	 * This method is used to keeping update information of space-craft every time you press a button. 
	 * 
	 * @param code
	 */
	public void doUpdateShip(int code)
	{
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		try{

			dos.write(ownShipID.getAddress().getAddress());
			dos.writeInt(gamePlaySocket.getLocalPort());
			dos.writeInt(code);
			dos.writeInt(sector.ownShip.getXPosition());
			dos.writeInt(sector.ownShip.getYPosition());
			dos.writeInt(sector.ownShip.getHeading());
	       
		}catch(IOException e)
		{
			System.out.println("Updating ship");
		}
		
		DatagramPacket packet = new DatagramPacket(baos.toByteArray(),baos.size());
		packet.setAddress(Constants.SERVER_IP);
		packet.setPort(Constants.SERVER_PORT);

		try{
			gamePlaySocket.send(packet);
		} catch (IOException e) {
		   System.out.println("doUpdateShip()");
		}
	}
	
	
	/*
	 * Starts the space game. Driver for the application.
	 */
	public static void main(String[] args) 
	{	
		new SpaceGameClient();
				
	} // end main
	
	
} // end SpaceGame class
