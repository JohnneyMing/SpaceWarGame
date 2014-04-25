import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import spaceWar.Constants;
import spaceWar.Obstacle;

import spaceWar.SpaceCraft;
import spaceWar.Torpedo;

/**
 * @author MINGWEI ZHONG
 * 
 * Class to listen for clients sending information reliably 
 * using TCP. It takes care of the following events:
 * 1. Client coming into the game
 * 2. Client firing torpedoes
 * 3. Client leaving the game
 * 4. Sending remove messages to the client
 */
public class PersistentConnectionToClient extends Thread {
 	
	
    DataInputStream dis = null;
    DataOutputStream dos = null;
    
    InetSocketAddress clientID;
    
    int port;
    int code;

	Socket clientConnection = null;
	
	SpaceGameServer spaceGameServer;
	
	boolean thisClientIsPlaying = true;
	
	public PersistentConnectionToClient(Socket sock, SpaceGameServer spaceGameServer) {
		
		this.clientConnection = sock;
		this.spaceGameServer = spaceGameServer;

	} // end PersistentConnectionToClient

	
	/**
	 * Listens for join and exiting clients using TCP. Joining clients are sent
	 * the x and y coordinates of all obstacles followed by a negative number. Receives
	 * fire messages from clients and the exit code when a client is leaving the game.
	 */
	public void run(){
			
			//TODO

			while( thisClientIsPlaying && spaceGameServer.playing ){ // loop till playing is set to false
			//TODO 
			try {
		
				dis = new DataInputStream(clientConnection.getInputStream());
				dos = new DataOutputStream(clientConnection.getOutputStream());
				
				
				code = dis.readInt();
				port = dis.readInt();
		
				clientID = new InetSocketAddress(clientConnection.getInetAddress(), port);
				
				if(code == Constants.REGISTER)
				{
				    
				    spaceGameServer.addPersistentConnection(this);
					InitializeGame();
				}
				
				else if(code == Constants.FIRED_TORPEDO)
				{
					FiringTorpedo();
				}
				else if(code == Constants.EXIT)
				{
					leavingGame();
				}
				
			} catch (IOException e) {}}
			
			
			//TODO
	} // end run
	
	
	/**
	 * Initializing the game at the beginning when each client establish the connection.
	 * 
	 */
	public void InitializeGame() {
		
		System.out.println("New Client: " + clientID);
		spaceGameServer.addClientDatagramSocketAddresses(clientID);
		ArrayList<Obstacle> obstacles = spaceGameServer.sector.getObstacles();
		
        for(Obstacle obs : obstacles)
        {
        	try {
				dos.writeInt(obs.getXPosition());
				dos.writeInt(obs.getYPosition());
			} catch (IOException e) {
				System.out.println("InitializeGame()");
			}      	
        }
	    try {
			dos.writeInt(-1);
		} catch (IOException e) {
			System.out.println("HandleNewClient()");
		}
	}
	
	/**
	 * Client is firing torpedo
	 * 
	 */
	protected void FiringTorpedo() {
		System.out.println("Client is firing torpedo");
		
		int x;
		try {
			x = dis.readInt();
			int y = dis.readInt();
			int heading = dis.readInt();
			Torpedo torpedo = new Torpedo(clientID,x,y,heading);
			spaceGameServer.sector.updateOrAddTorpedo(torpedo);

		} catch (IOException e) {
			System.out.println("handleTorpedoLanuch()");
		}
	}
	
	/**
	 * Method which is used to handle when client is leaving the game.
	 * 
	 * 
	 */
	protected void leavingGame()
	{
		System.out.println("This client is leaving the game");
		spaceGameServer.removeClientDatagramSocketAddresses(clientID);
		SpaceCraft s = new SpaceCraft(clientID);
		spaceGameServer.sendRemoves(s);		
		try {
			dis.close();
			dos.close();
			clientConnection.close();
		} catch (IOException e) {
			System.out.println("leavingGame()");
		}
	}

	/**
	 * Send the remove message to client
	 * 
	 * @param sc the craft you intended to remove.
	 */
	protected void sendRemoveToClient(SpaceCraft sc )
	{
		
		//TODO
		try
		{
			dos.write(sc.ID.getAddress().getAddress());
			dos.writeInt(sc.ID.getPort());
	       if(sc instanceof Torpedo)
	       {
				dos.writeInt(Constants.REMOVE_TORPEDO);
	       }
	       else
	       {
				dos.writeInt(Constants.REMOVE_SHIP);
	       }
	       
	} catch (IOException e)
	{
	
	}
		
}	
		// end sendRemoveToClient
	
	
} // end PersistentConnectionToClient class
