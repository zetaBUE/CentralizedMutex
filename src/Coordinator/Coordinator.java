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
    private static final int[] RESOURCE_PORTS = {6000, 6001}; // Shirts → 6000, Pants → 6001

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
             PrintWriter out = new PrintWriter(store.getOutputStream(), true)) {

            out.println("Access granted.");
            
            String received = in.readLine();
            if (received == null) {
                System.out.println("Client disconnected before choosing a server.");
                return;
            }
        
            int chosenPort = Integer.parseInt(in.readLine());

            if (chosenPort != 6000 && chosenPort != 6001) {
                out.println("Coordinator: Invalid choice. Defaulting to 6000.");
                chosenPort = 6000;
            }

            out.println("Coordinator: Proceed to connect to Storage Server on port " + chosenPort);
            String message = in.readLine();
            
            if (message == null) {
                System.out.println("Client disconnected before sending RELEASE.");
                return;
            }

            if ("RELEASE".equalsIgnoreCase(message)) {
                resourceLocked = false;
                out.println("Coordinator: Resource released. Next store will be granted access.");
                processQueue();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
