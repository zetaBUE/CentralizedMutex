/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Server;

/**
 *
 * @author zeta440
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class StorageServer {
    private int port;
    private String productCategory;
    private List<Product> inventory = new ArrayList<>();

    public StorageServer(int port, String category) {
        this.port = port;
        this.productCategory = category;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Storage Server for " + productCategory + " running on port " + port);

            while (true) {
                Socket storeSocket = serverSocket.accept();
                new Thread(() -> handleRequest(storeSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRequest(Socket store) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(store.getInputStream()));
                PrintWriter output = new PrintWriter(store.getOutputStream(), true)) {

            output.println("Connected to Storage Server for " + productCategory
                    + ". Choose: ADD <item> <qty>, DELETE <item>, VIEW");

            String request = input.readLine();
            String[] parts = request.split(" ");

            if (parts[0].equalsIgnoreCase("ADD")) {
                String name = parts[1];
                int qty = Integer.parseInt(parts[2]);
                inventory.add(new Product(name, productCategory, qty));
                output.println("Added " + qty + " " + name + " to " + productCategory + " inventory.");
            } else if (parts[0].equalsIgnoreCase("DELETE")) {
                String name = parts[1];
                inventory.removeIf(p -> p.getName().equalsIgnoreCase(name));
                output.println("Deleted " + name + " from inventory.");
            } else if (request.equalsIgnoreCase("VIEW")) {
                if (inventory.isEmpty()) {
                    output.println("Inventory is empty.");
                } else {
                    output.println("Inventory: " + this.inventory.toString());
                }
            } else {
                output.println("Invalid request.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Thread(() -> new StorageServer(6000, "Shirts").startServer()).start();
        new Thread(() -> new StorageServer(6001, "Pants").startServer()).start();
    }
}
