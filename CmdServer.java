import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer; //import java.util.Random;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.WriteCoilRequest;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.msg.*;

class ACommand {
	public int OutputCommand;
	public int InputSensor;
	public int WaitTime;

	public ACommand() {
		this.OutputCommand = 0;
		this.InputSensor = 0;
		this.WaitTime = 0;
	}

	public ACommand(int OC, int IS, int WT) {
		this.OutputCommand = OC;
		this.InputSensor = IS;
		this.WaitTime = WT;
	}

	public ACommand(String nextToken, String nextToken2, int inWaitTime) {
		this(Integer.parseInt(nextToken), Integer.parseInt(nextToken2),
				inWaitTime);
	}

	public String ToString() {
		String mytext = "";
		mytext = "Move " + this.OutputCommand + " until " + this.InputSensor
				+ " and wait " + this.WaitTime + " milliseconds";
		return mytext;
	}
}

public class CmdServer {

	static TCPMasterConnection conn = null;
	static String ip = null;
	static int unitID = 0;
	static int timeout = 100;
	static ModbusTCPTransaction myTransaction = null;
	static ServerSocket myServer = null;
	static Boolean asServer = false;

	public static void main(String[] args) {
		// if (args.length==1){
		if (args[0].contains("erver")) {
			ip = args[1];
			unitID = Integer.parseInt(args[2]);
			System.out.println("Running Server on " + ip + ";");
			asServer = true;

		} else {
			PrintHelp();
			return;
		}
		// }
		// else{
		// if (args.length < 4) {
		// PrintHelp();
		// return;
		// }
		// }

		if (asServer) {
			// String outCommands = new String("-1/3/-4/-5/4/-2/05");
			// String inCommands = new String("-5/05/-3/1/2");
			// String outSensors = new String("01/9/06/11/8/05/12");
			// String inSensors = new String("11/12/10/0/7");

			try {
				myServer = new ServerSocket(33123);
				System.out.println("Created Command Server");
			} catch (IOException e1) {
				System.out.println("Unable To Create Command Server");
				myServer = null;
			}

			while (true) {
				try {
					Socket connection = myServer.accept();
					InetAddress ia = connection.getInetAddress();

					System.out.println("	Socket Accepted on "
							+ ia.getHostAddress());
					ProcessCommands(connection);
					connection.close();
					System.out.println("	Socket Closed");
				} catch (IOException e) {
					System.out.println("	Socket Error");
				}

				break;
			}
		} else {
			StringTokenizer OutputCommandTokens = new StringTokenizer(args[1],
					"/");
			StringTokenizer SensorCommandTokens = new StringTokenizer(args[2],
					"/");
			if (OutputCommandTokens.countTokens() != SensorCommandTokens
					.countTokens()) {
				System.out
						.println("Usage: ModBus ip OutputSequence SensorSequence timeout");
				System.out.println(" Exception - OutputSequence length ("
						+ OutputCommandTokens.countTokens()
						+ ") must equal SensorSequence length ("
						+ SensorCommandTokens.countTokens() + ")");
				return;
			}

			ip = args[0];
			// unitID = Integer.parseInt(args[2]);
			timeout = Integer.parseInt(args[3]);

			System.out.println("Starting Command Server w " + args.length
					+ " args");

			ArrayList<ACommand> myCommands = new ArrayList<ACommand>(20);
			while (OutputCommandTokens.hasMoreTokens()) {
				myCommands.add(new ACommand(OutputCommandTokens.nextToken(),
						SensorCommandTokens.nextToken(), timeout));
			}

			try {
				for (ACommand thiscommand : myCommands) {
					System.out.println(thiscommand.ToString());
					if (thiscommand.OutputCommand < 0) {
						TurnOffCoil((0 - thiscommand.OutputCommand));
					} else {
						TurnOnCoil(thiscommand.OutputCommand);
					}

					System.out.println("Sensor Wait");
					try {
						if (thiscommand.InputSensor > 0) {
							WaitForSensor(thiscommand.InputSensor);
						}
					} catch (ModbusException e) {
						System.out.println("Modbus Failed");
					}

					System.out.println("Time Wait");

					try {
						Thread.sleep(timeout);
					} catch (InterruptedException e) {

					}
				}
			} catch (ConnectException e) {
				System.out.println("Failed To Connect");
			} catch (Exception e) {
				System.out.println("Unknown Errors");

			} finally {
				if (conn != null)
					conn.close();
			}
		}

		System.out.println("Done");

	}

	private static String ReadSensors() throws ModbusException {
		try {
			char[] SensorsA;
			char[] SensorsB;
			String SensorOutput;
			// System.out.println("ReadSensors()");
			ReadInputDiscretesRequest req = null;
			ReadInputDiscretesResponse res = null;
			req = new ReadInputDiscretesRequest(0, 8);
			req.setUnitID(1);
			// System.out.println(" Request (a) Built");
			if (myTransaction == null) {
				myTransaction = new ModbusTCPTransaction(conn);
				System.out.println(" Created New Transaction");
			}
			myTransaction.setRequest(req);
			// System.out.println(" Request Set");
			myTransaction.execute();
			// System.out.println(" Executed");
			res = (ReadInputDiscretesResponse) myTransaction.getResponse();
			SensorsA = res.getDiscretes().toString().toCharArray();
			SensorOutput = "";
			for (int i = SensorsA.length - 1; i > 0; i--) {
				SensorOutput = SensorOutput + SensorsA[i - 1];
			}

			req = new ReadInputDiscretesRequest(8, 8);
			req.setUnitID(1);
			// System.out.println(" Request (b) Built");
			if (myTransaction == null) {
				myTransaction = new ModbusTCPTransaction(conn);
				System.out.println(" Created New Transaction");
			}
			myTransaction.setRequest(req);
			// System.out.println(" Request Set");
			myTransaction.execute();
			// System.out.println(" Executed");
			res = (ReadInputDiscretesResponse) myTransaction.getResponse();
			SensorsB = res.getDiscretes().toString().toCharArray();
			for (int i = SensorsB.length - 1; i > 0; i--) {
				SensorOutput = SensorOutput + SensorsB[i - 1];
			}
			// System.out.println("Sensor Read: *" + SensorOutput + "*");
			return SensorOutput;
		} catch (Exception e) {
			System.out.println("Sensor Read Error");
		}
		return "ERROR\r\n";
	}

	private static void WaitForSensor(int inputSensor) throws ModbusException {
		inputSensor = inputSensor - 1;
		ReadInputDiscretesRequest req = null;
		ReadInputDiscretesResponse res = null;
		boolean foundstate = false;
		System.out.println("Watching Sensor " + inputSensor);
		while (foundstate == false) {
			try {
				req = new ReadInputDiscretesRequest(inputSensor, 1);
				req.setUnitID(1);
				myTransaction.setRequest(req);
				myTransaction.execute();
				res = (ReadInputDiscretesResponse) myTransaction.getResponse();
				try {
					if (res.getDiscretes().getBit(0)) {
						foundstate = true;
					}
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Unable to find sensor bit");
				}
				// System.out.println("Digital Inputs Status=" +
				// res.getDiscretes().toString());

			} catch (ModbusIOException e) {
				System.out.println("Modbus IO Exception");
			} catch (ModbusSlaveException e) {
				System.out.println("Modbus Slave Exception");
			} catch (ModbusException e) {
				System.out.println("Unknown Modbus Exception");
			} catch (Exception e) {
				System.out.println("Unknown Exception In WaitForSensor");
				throw new ModbusException();
			}
		}
		System.out.println("Found Sensor" + inputSensor);
	}

	public static void OpenConnection() {
		System.out.println("Opening Connection");
		try {
			InetAddress address = InetAddress.getByName(ip);
			conn = new TCPMasterConnection(address);
			conn.setPort(Modbus.DEFAULT_PORT);
			conn.connect();
			System.out.println("Opened");
		} catch (ConnectException e) {
			System.out.println("\tUnable To Connect");
			conn = null;
		} catch (UnknownHostException e) {
			System.out.println("\tUnknown Host");
			conn = null;
		} catch (Exception e) {
			System.out.println("\tUnknown Connection Error");
			conn = null;
		} finally {

		}
	}

	public static void TurnOnCoil(int outputCommand) throws ConnectException {
		System.out.println("Turning On Coil " + outputCommand);
		CommandCoil(outputCommand, true);
	}

	public static void TurnOffCoil(int outputCommand) throws ConnectException {
		System.out.println("Turning Off Coil " + outputCommand);
		CommandCoil(outputCommand, false);
	}

	public static void CommandCoil(int outputCommand, boolean b)
			throws ConnectException {
		if (conn == null) {
			OpenConnection();
			if (conn == null) {
				System.out.println("Unable To Connect");
				throw new ConnectException();
			}
		}
		try {
			WriteCoilRequest wcr = new WriteCoilRequest(outputCommand - 1, b);
			wcr.setUnitID(1); // 0 - WAGO; 1 - ADAM6050
			myTransaction = new ModbusTCPTransaction(conn);
			myTransaction.setRequest(wcr);
			myTransaction.execute();
		} catch (ModbusIOException e) {
			System.out.println("Modbus IO Exception");
		} catch (ModbusSlaveException e) {
			System.out.println("Modbus Slave Exception");
		} catch (ModbusException e) {
			System.out.println("Unknown Modbus Exception");
		}
		System.out.println("Commanded (Actual) Coil " + (outputCommand - 1));
	}

	public static void PrintHelp() {
		System.out.println("Client Mode Usage:");
		System.out
				.println("ModBus ip UnitID OutputSequence SensorSequence timeout");
		System.out.println("   ip             - IP Address of ModBus coupler");
		System.out.println("   UnitID         - Unit ID of ModBus coupler");
		System.out
				.println("   OutputSequence - / delimited list of outputs to run (- for off)");
		System.out
				.println("   SensorSequence - / delimited list of sensors to monitor (0 for no wait)");
		System.out.println("   timeout        - milliseconds between commands");
		System.out.println("");
		System.out.println("Server Mode Usage:");
		System.out.println("ModBus Server");
		System.out
				.println("   Server         - This keyword triggers server mode");
		System.out.println("Notes:");
		System.out.println(" - ...");
	}

	public static ArrayList<ACommand> ParseCommands(String myCommands,
			String mySensors) {
		ArrayList<ACommand> samplecommands = new ArrayList<ACommand>(20);

		return samplecommands;
	}

	public static boolean CheckConnection() {
		if (conn == null)
			return false;
		if (!conn.isConnected())
			return false;
		return true;
	}

	public static boolean WriteBoth(OutputStreamWriter outStream, String toSend) {
		try {
			outStream.write(toSend);
			outStream.flush();
			System.out.print(toSend);
		} catch (IOException e) {
			System.out.println("***Unable To Send***");
			return false;
		}
		return true;
	}

	public static void ProcessCommands(Socket myConnection) {
		OutputStreamWriter out;
		InputStreamReader in;

		System.out.println("	Establishing Streams");
		try {
			out = new OutputStreamWriter(myConnection.getOutputStream());
			in = new InputStreamReader(myConnection.getInputStream());
		} catch (IOException e1) {
			System.out.println("Unable To Handle Streams");
			return;
		}

		System.out.println("	Established Streams");

		Boolean ServerRunning = true;
		char nextchar;
		String nextCommand;

		WriteBoth(out, "Server Connected\r\n");

		while (ServerRunning) {
			System.out.println("    Waiting for next command");
			nextCommand = "";
			nextchar = ' ';
			while (nextchar != 10) {
				try {
					nextchar = (char) in.read();
					// System.out.println("*" + ((int) nextchar) + "*");
				} catch (IOException e) {
					System.out
							.println("Unable To Read Characters From Input Stream");
					return;
				}
				nextCommand = nextCommand + nextchar;
			}
			/*
			 * try { in.read(); } catch (IOException e1) {
			 * System.out.println("Unable To Read Characters From Input Stream"
			 * ); } // Read off linefeed
			 */
			// nextCommand = nextCommand + '\0';
			System.out.print("    Got Command " + nextCommand);

			if (nextCommand.startsWith("set")) {
				if (!CheckConnection()) {
					WriteBoth(out, "Unable to set; Bad Connection\r\n");
					continue;
				}
				int toCommand = nextCommand.charAt(3) - 48;
				WriteBoth(out, "Setting *" + toCommand + "*\r\n");

				try {
					CommandCoil(toCommand, true);
				} catch (ConnectException e) {
					WriteBoth(out, "Connection Failure While Setting");
				}
				continue;
			}
			if (nextCommand.startsWith("reset")) {
				if (!CheckConnection()) {
					WriteBoth(out, "Unable to reset; Bad Connection\r\n");
					continue;
				}
				int toCommand = nextCommand.charAt(5) - 48;
				WriteBoth(out, "Resetting *" + toCommand + "*\r\n");
				try {
					CommandCoil(toCommand, false);
				} catch (ConnectException e) {
					WriteBoth(out, "Connection Failure While Resetting");
				}
				continue;
			}
			if (nextCommand.compareToIgnoreCase("status\r\n") == 0) {
				if (conn == null) {
					WriteBoth(out, "No Connection\r\n");
				} else {
					if (!conn.isConnected()) {
						WriteBoth(out, "Disconnected\r\n");
					} else {
						WriteBoth(out, "Connected Normally\r\n");
					}
				}
				continue;
			}
			if (nextCommand.compareToIgnoreCase("start\r\n") == 0) {
				System.out.println("    Processing Start");
				if (conn == null) {
					OpenConnection();
					if (conn == null) {
						WriteBoth(out, "Unable To Connect\r\n");
					} else {
						WriteBoth(out, "WAGO Connected\r\n");
					}
				}
				continue;
			}
			if (nextCommand.compareToIgnoreCase("state\r\n") == 0) {
				// System.out.println("	 Processing State");
				if (!CheckConnection()) {
					WriteBoth(out, "Unable to check state; No Connection\r\n");
					continue;
				}
				try {
					System.out.print("    Sensors: ");
					WriteBoth(out, ReadSensors() + "\r\n");
				} catch (ModbusException e) {
					WriteBoth(out, "Unable To Read Sensors\r\n");
				}
				continue;
			}
			if (nextCommand.compareToIgnoreCase("quit\r\n") == 0) {
				WriteBoth(out, "Bye\r\n");
				if (conn != null) {
					conn.close();
					conn = null;
				}
				ServerRunning = false;
				System.out.println("Shutting Down");
				continue;
			}
			WriteBoth(out, "Unknown Command\r\n");
		}
	}

}
