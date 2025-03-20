/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Coordinator;

/**
 *
 * @author zeta440
 */
import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class Coordinator {
    private static final int PORT = 5000;
    private Queue<Socket> requestQueue = new LinkedList<>();
    private boolean resourceLocked = false;
    private static final int[] RESOURCE_PORTS = { 6000, 6001 }; // Shirts → 6000, Pants → 6001

    public static void main(String[] args) {
        new Coordinator().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Coordinator running on port " + PORT);

            while (true) {
                Socket storeSocket = serverSocket.accept();
                synchronized (requestQueue) {
                    requestQueue.add(storeSocket);
                }
                processQueue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processQueue() {
        synchronized (requestQueue) {
            if (!resourceLocked && !requestQueue.isEmpty()) {
                Socket store = requestQueue.poll();
                resourceLocked = true;
                new Thread(() -> handleStore(store)).start();
            }
        }
    }

    private void handleStore(Socket store) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(store.getInputStream()));
                DataInputStream dataIn = new DataInputStream(store.getInputStream());
                PrintWriter out = new PrintWriter(store.getOutputStream(), true)) {

            String request = in.readLine();
            System.out.println("Received request: " + request);

            if ("REQUEST".equals(request)) {
                // Handle operation request
                synchronized (requestQueue) {
                    if (!resourceLocked) {
                        resourceLocked = true;
                        out.println("Granted");
                        System.out.println("Operation access granted");

                        // Add 3-second delay to make mutex visible
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // Wait for RELEASE
                        String releaseMsg = in.readLine();
                        if ("RELEASE".equalsIgnoreCase(releaseMsg)) {
                            resourceLocked = false;
                            out.println("Released");
                            System.out.println("Operation completed, mutex released");
                            processQueue();
                        }
                    } else {
                        out.println("Denied");
                        System.out.println("Operation access denied - resource locked");
                    }
                }
            } else if ("CONNECT".equals(request)) {
                // Handle initial connection request - no mutex needed
                out.println("Granted");
                System.out.println("Initial connection granted");

                // Read the chosen port using DataInputStream
                int chosenPort = dataIn.readInt();
                System.out.println("Received port request: " + chosenPort);

                if (chosenPort != 6000 && chosenPort != 6001) {
                    out.println("Invalid choice. Defaulting to 6000.");
                    chosenPort = 6000;
                } else {
                    out.println("Connected to Storage Server on port " + chosenPort);
                }

                // Read the RELEASE message for connection
                String message = in.readLine();
                if ("RELEASE".equalsIgnoreCase(message)) {
                    out.println("Connection completed");
                    System.out.println("Initial connection completed for port " + chosenPort);
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
            synchronized (requestQueue) {
                resourceLocked = false;
                processQueue();
            }
        } finally {
            try {
                store.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
