package client.view;

import java.util.Scanner;
import java.util.StringJoiner;

import client.controller.NetworkController;
import client.net.OutputHandler;

/**
 * Reads and interprets user commands. The command interpreter will run in a separate thread, which
 * is started by calling the <code>start</code> method. Commands are executed in a thread pool, a
 * new prompt will be displayed as soon as a command is submitted to the pool, without waiting for
 * command execution to complete.
 * 
 */
public class NonBlockingInterpreter implements Runnable {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private boolean receivingCmds = false;
    private NetworkController contr;
    private final ThreadSafeStdOut outMgr = new ThreadSafeStdOut();

    /**
     * Starts the interpreter. The interpreter will be waiting for user input when this method
     * returns. Calling <code>start</code> on an interpreter that is already started has no effect.
     */
    public void start() {
        if (receivingCmds) {
            return;
        }
        receivingCmds = true;
        contr = new NetworkController();
        new Thread(this).start();
    }

    /**
     * Interprets and performs user commands.
     */
    @Override
    public void run() {
        while (receivingCmds) {
            try {
                CmdLineHandler cmdLine = new CmdLineHandler(readNextLine());
                switch (cmdLine.getCmd()) {
                    case QUIT:
                        receivingCmds = false;
                        contr.disconnect();
                        break;
                    case CONNECT:
                        contr.connect(cmdLine.getParameter(0),
                                      Integer.parseInt(cmdLine.getParameter(1)),
                                      new ConsoleOutput());
                        break;
                    case USER:
                        contr.sendUsername(cmdLine.getParameter(0));
                        break;
                    case START:
                        contr.start();
                        break;
                    default:
                        contr.sendInput(cmdLine.getUserInput());
                }
            } catch (Exception e) {
                outMgr.println("Operation failed");
            }
        }
    }

    private String readNextLine() {
        outMgr.print(PROMPT);
        return console.nextLine();
    }
    /*
     * @role: the main logic for UI
     * @UI logic: 
     * 		firstly check the game state( win, lose, continue), different system notice for different state
     * 		then print all the details of the game status
     * 		all the print are thread safe by using the thread safe standard output
     */
    private class ConsoleOutput implements OutputHandler {
        @Override
        public void handleMsg(String msg) {
        	if(msg.contains(" ")) {
        		outMgr.println(msg);
        		return;
        	}
        	GameStatus gameStatus=new GameStatus(msg);
        	if(gameStatus.getCurrentGameState().equals("win")) {
        		outMgr.println("Congratulations! You won this game!");
        	}else {
        		if(gameStatus.getCurrentGameState().equals("lose")) {
        			outMgr.println("Sorry you lose");
        		}
        		else {
        			outMgr.println("Please continue!");
        		}
        	}
        	outMgr.println("Dear "+gameStatus.getPlayerName());
        	outMgr.println("Your current score is:"+gameStatus.getScore());
        	outMgr.println("Your left attempts:"+gameStatus.getAttempts());
        	outMgr.println("Hint:"+gameStatus.getHintWord());
            outMgr.print(PROMPT);
        }
    }
}
