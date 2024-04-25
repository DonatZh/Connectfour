package com.example.connectfour;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

public class ConnectFourClient extends JPanel implements Runnable, ConnectFourConstraints {

    private boolean myTurn = false;
    private char myToken = ' ';
    private char otherToken = ' ';
    private Cell[][] cell = new Cell[6][7];
    private JLabel jlblTitle = new JLabel();
    private JLabel jlblStatus = new JLabel();
    private JOptionPane readyWindow = new JOptionPane();
    private int rowSelected;
    private int columnSelected;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private boolean continueToPlay = true;
    private boolean waiting = true;
    private String host = "localhost";

    public void createComponents() {
        setLayout(new BorderLayout());

        // Panel for cells
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(6, 7, 0, 0));
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                p.add(cell[i][j] = new Cell(i, j, cell));
            }
        }

        // Set properties for labels and borders for labels and panel
        p.setBorder(new LineBorder(Color.black, 1));
        jlblTitle.setHorizontalAlignment(JLabel.CENTER);
        jlblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        jlblTitle.setBorder(new LineBorder(Color.black, 1));
        jlblStatus.setBorder(new LineBorder(Color.black, 1));

        add(jlblTitle, BorderLayout.NORTH);
        add(p, BorderLayout.CENTER);
        add(jlblStatus, BorderLayout.SOUTH);
        connectToServer();
    }

    private void connectToServer() {
        try {
            // Create a socket to connect to the server
            Socket socket = new Socket(host, 1234);

            // Create an input stream to receive data from the server
            fromServer = new DataInputStream(socket.getInputStream());

            // Create an output stream to send data to the server
            toServer = new DataOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            // Display an error message if the connection fails
            JOptionPane.showMessageDialog(this, "Failed to connect to the server: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0); // Exit the program
        }

        // Control the game on a separate thread
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        try {
            // Get notification from the server
            int player = fromServer.readInt();

            if (player == PLAYER1) {
                myToken = 'r';
                otherToken = 'b';
                jlblTitle.setText("Player 1 - Color red");
                jlblStatus.setText("Waiting for player 2 to join");

                // Receive startup notification from the server
                fromServer.readInt();

                // The other player has joined
                jlblStatus.setText("Player 2 has joined. I start first");

                // It is my turn
                myTurn = true;
            } else if (player == PLAYER2) {
                myToken = 'b';
                otherToken = 'r';
                jlblTitle.setText("Player 2 - Color blue");
                jlblStatus.setText("Waiting for player 1 to move");
            }

            while (continueToPlay) {
                if (player == PLAYER1) {
                    waitForPlayerAction();
                    sendMove();
                    receiveInfoFromServer();
                } else if (player == PLAYER2) {
                    receiveInfoFromServer();
                    waitForPlayerAction();
                    sendMove();
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private void waitForPlayerAction() throws IOException {
        while (waiting) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        waiting = true;
    }

    private void sendMove() throws IOException {
        toServer.writeInt(rowSelected);
        toServer.writeInt(columnSelected);
    }

    private void receiveInfoFromServer() throws IOException {
        int status = fromServer.readInt();

        if (status == PLAYER1_WON) {
            continueToPlay = false;
            if (myToken == 'r') {
                jlblStatus.setText("I won! (red)");
            } else if (myToken == 'b') {
                jlblStatus.setText("Player 1 (red) has won!");
                receiveMove();
            }
        } else if (status == PLAYER2_WON) {
            continueToPlay = false;
            if (myToken == 'b') {
                jlblStatus.setText("I won! (blue)");
            } else if (myToken == 'r') {
                jlblStatus.setText("Player 2 (blue) has won!");
                receiveMove();
            }
        } else if (status == DRAW) {
            continueToPlay = false;
            jlblStatus.setText("Game is over, no winner!");

            if (myToken == 'b') {
                receiveMove();
            }
        } else {
            receiveMove();
            jlblStatus.setText("My turn");
            myTurn = true;
        }
    }

    private void receiveMove() throws IOException {
        int row = fromServer.readInt();
        int column = fromServer.readInt();
        cell[row][column].setToken(otherToken);
    }

    public class Cell extends JPanel {

        private int row;
        private int column;
        private char token;

        public Cell(int row, int column, Cell[][] cell) {
            this.row = row;
            this.column = column;
            setBorder(new LineBorder(Color.black, 1));
            addMouseListener(new ClickListener());
        }

        public char getToken() {
            return token;
        }

        public void setToken(char c) {
            token = c;
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (token == 'r') {
                g.setColor(Color.RED);
                g.fillOval(10, 10, getWidth() - 20, getHeight() - 20);
            } else if (token == 'b') {
                g.setColor(Color.BLUE);
                g.fillOval(10, 10, getWidth() - 20, getHeight() - 20);
            }
        }


        private class ClickListener extends MouseAdapter {
            public void mouseClicked(MouseEvent e) {
                int r = -1;
                for (int x = 5; x >= 0; x--) {
                    if (cell[x][column].getToken() == '\u0000') {

                        r = x;
                        break;
                    }
                }

                if ((r != -1) && myTurn) {
                    cell[r][column].setToken(myToken);
                    myTurn = false;
                    rowSelected = r;
                    columnSelected = column;
                    jlblStatus.setText("Waiting for the other player to move");
                    waiting = false;
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Connect Four");
        ConnectFourClient connectFourClient = new ConnectFourClient();
        connectFourClient.createComponents();
        frame.getContentPane().add(connectFourClient);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setVisible(true);
    }
}
