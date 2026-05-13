import java.io.*;
import java.util.*;

/**
 * ============================================================
 *  MotoVault - Motorcycle Inventory & Rental Tracker
 * ============================================================
 *  Roles  : ADMIN (full access), STAFF (limited), USER (view)
 *  Cipher : Caesar Cipher (shift = 3)
 *  Storage: 1D & 2D arrays + flat text files
 *
 *  FLOW:
 *   Main Menu  →  [Login] [Register] [Exit]
 *   After login, role is detected from credentials and the
 *   correct menu (Admin / Staff / User) is opened automatically.
 *
 *  RENTAL FLOW:
 *   User reserves a bike  →  Status: PENDING
 *   Staff confirms + processes payment  →  Status: CONFIRMED
 *   Staff prints receipt  →  Rental record created  →  Status: ACTIVE
 *   Staff processes return  →  Status: RETURNED
 * ============================================================
 */
public class Test {

    // ─────────────────────────── CONSTANTS ────────────────────────────
    static final int MAX_BIKES      = 50;
    static final int MAX_USERS      = 30;
    static final int MAX_RENTALS    = 100;
    static final int MAX_FAVORITES  = 5;
    static final int CAESAR_SHIFT   = 3;

    // File paths
    static final String ADMIN_FILE  = "admin.txt";
    static final String STAFF_FILE  = "staff.txt";
    static final String USERS_FILE  = "users.txt";
    static final String DATA_FILE   = "data.txt";

    // ──────────────────────────── DATA ARRAYS ─────────────────────────
    // bikes[i] = { brand, model, type, color, engineCC, dailyRate, stock, bikeId }
    static String[][] bikes     = new String[MAX_BIKES][8];
    static int        bikeCount = 0;

    // users[i] = { username, encryptedPassword, role, email, mobile }
    static String[][] users     = new String[MAX_USERS][5];
    static int        userCount = 0;

    // rentals[i] = { rentalId, customerUser, bikeId, startDate, endDate,
    //                status, paymentMethod, totalCost, days }
    static String[][] rentals     = new String[MAX_RENTALS][9];
    static int        rentalCount = 0;

    // reservations[i] = { reserveId, customerUser, bikeId, startDate, endDate,
    //                     status, paymentMethod, totalCost, days }
    //   status values:  PENDING  →  CONFIRMED  (after staff processes payment)
    //                             →  rental record created automatically
    static String[][] reservations     = new String[MAX_RENTALS][9];
    static int        reservationCount = 0;

    // favorites[userIndex][0..4] = bikeId  (empty string = unused slot)
    static String[][] favorites = new String[MAX_USERS][MAX_FAVORITES];

    // ── Session state ──
    static String currentUser      = "";
    static String currentRole      = "";
    static int    currentUserIndex = -1;

    static Scanner sc = new Scanner(System.in);

    // ══════════════════════════ MAIN ══════════════════════════════════
    public static void main(String[] args) {
        loadFiles();
        printBanner();
        Mainmenu();
    }

    static void Mainmenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║         MAIN  MENU           ║");
            System.out.println("╠══════════════════════════════╣");
            System.out.println("║  [1] Login                   ║");
            System.out.println("║  [2] Register                ║");
            System.out.println("║  [3] Exit                    ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.print("  Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("  Proceed to Login? (Y/N): ");
                    String loginChoice = sc.nextLine().trim().toUpperCase();
                    if (loginChoice.equals("Y")) {
                        if (login()) {
                        // Route to the correct panel based on the detected role
                        handleCRUD();
                        }
                    } else {
                        System.out.println("Returning to main m  1enu...");
                        Mainmenu();
                    }
                    break;
                case "2":
                    System.out.print("  Proceed to Register? (Y/N): ");
                    String registerChoice = sc.nextLine().trim().toUpperCase();
                    if (registerChoice.equals("Y")) {  
                        registerUser();
                    } else {
                        System.out.println("Returning to main menu...");
                        Mainmenu();
                    }
                    break;
                case "3":
                    saveFiles();
                    System.out.println("\n  Thank you for using MotoVault. Ride safe!\n");
                    running = false;
                    break;
                default:
                    System.out.println("  [!] Invalid choice. Try again.");
            }
        }
    }

    // ══════════════════════════ BANNER ════════════════════════════════
    static void printBanner() {
        System.out.println("\n  ╔════════════════════════════════════════════╗");
        System.out.println("  ║         Welcome to  M O T O V A U L T      ║");
        System.out.println("  ║    Motorcycle Inventory & Rental System    ║");
        System.out.println("  ╚════════════════════════════════════════════╝");
    }

    static String generateBikeId(int index) {
        return "MV-" + String.format("%04d", index + 1) + "-01";
    }

    // ══════════════════════════ CAESAR CIPHER ═════════════════════════
    /** Encrypts plain text using Caesar Cipher (shift = CAESAR_SHIFT). */
    static String encrypt(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                sb.append((char) ((c - base + CAESAR_SHIFT) % 26 + base));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Decrypts a Caesar-encrypted string back to plain text. */
    static String decrypt(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                sb.append((char) ((c - base - CAESAR_SHIFT + 26) % 26 + base));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ══════════════════════════ LOGIN ══════════════════════════════════
    /**
     * Unified login: searches all roles. Sets currentUser, currentRole,
     * and currentUserIndex on success.
     *
     * @return true if credentials matched any user record, false otherwise.
     */
    static boolean login() {
        System.out.println("\n  ─── LOGIN ───");
        System.out.println("  (Enter 0 at any field to cancel)");
        System.out.print("  Username : ");
        String username = sc.nextLine().trim();
        if (username.equals("0")) { 
            System.out.println("  Login cancelled."); 
            return false; 
        }
        System.out.print("  Password : ");
        String password = sc.nextLine().trim();
        String encPwd = encrypt(password);
        for (int i = 0; i < userCount; i++) {
            if (users[i][0].equals(username) && users[i][1].equals(encPwd)) {
                currentUser      = username;
                currentRole      = users[i][2];
                currentUserIndex = i;
                System.out.println("  [✓] Login successful! Welcome, " + username
                                   + "  [Role: " + currentRole + "]");
                return true;
            }
        }
        System.out.println("  [✗] Invalid username or password.");
        return false;
    }

    // ══════════════════════════ VALIDATE INPUT ═════════════════════════
    /**
     * Validates an input string against a regular-expression pattern.
     *
     * @param input the string to test
     * @param regex the regex pattern
     * @return true when input matches regex exactly, false otherwise
     */
    static boolean validateInput(String input, String regex) {
        return input != null && input.matches(regex);
    }

    // ═════════════════════ HANDLE CRUD (ROUTER) ════════════════════════
    /**
     * Routes the logged-in session to the correct role menu.
     * Called immediately after a successful login().
     */
    static void handleCRUD() {
        switch (currentRole) {
            case "ADMIN": adminMenu(); break;
            case "STAFF": staffMenu(); break;
            case "USER":  userMenu();  break;
            default:
                System.out.println("  [!] Unknown role. Access denied.");
                logoutSession();
        }
    }

    // ══════════════════════════ FILE I/O ═══════════════════════════════
    /** Loads all persistent data files into the in-memory arrays. */
    static void loadFiles() {
        loadCredentialFile(ADMIN_FILE, "ADMIN");
        loadCredentialFile(STAFF_FILE, "STAFF");
        loadUsersFile();
        loadDataFile();
    }

    /** Persists all in-memory arrays back to their text files. */
    static void saveFiles() {
        saveCredentialFile(ADMIN_FILE, "ADMIN");
        saveCredentialFile(STAFF_FILE, "STAFF");
        saveUsersFile();
        saveDataFile();
    }

    /** Reads admin.txt or staff.txt and fills the users array for that role. */
    static void loadCredentialFile(String filename, String role) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null && userCount < MAX_USERS) {
                String[] p = line.split("\\|", -1);
                if (p.length == 5 && p[2].equals(role)) {
                    for (int j = 0; j < 5; j++) users[userCount][j] = p[j];
                    for (int j = 0; j < MAX_FAVORITES; j++) favorites[userCount][j] = "";
                    userCount++;
                }
            }
        } catch (IOException ignored) { /* file created on first save */ }
    }

    /** Reads users.txt and loads USER-role accounts (includes favourites). */
    static void loadUsersFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null && userCount < MAX_USERS) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 5 && p[2].equals("USER")) {
                    for (int j = 0; j < 5; j++) users[userCount][j] = p[j];
                    for (int j = 0; j < MAX_FAVORITES; j++)
                        favorites[userCount][j] = (p.length > 5 + j) ? p[5 + j] : "";
                    userCount++;
                }
            }
        } catch (IOException ignored) { }
    }

    /**
     * Reads data.txt (sections [BIKES], [RENTALS], [RESERVATIONS])
     * and populates the corresponding arrays.
     */
    static void loadDataFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            String section = "";
            while ((line = br.readLine()) != null) {
                if (line.equals("[BIKES]"))        { section = "BIKES";        continue; }
                if (line.equals("[RENTALS]"))      { section = "RENTALS";      continue; }
                if (line.equals("[RESERVATIONS]")) { section = "RESERVATIONS"; continue; }
                if (line.trim().isEmpty())          continue;

                String[] p = line.split("\\|", -1);
                if (section.equals("BIKES") && bikeCount < MAX_BIKES && p.length == 8) {
                    for (int j = 0; j < 8; j++) bikes[bikeCount][j] = p[j];
                    bikeCount++;
                } else if (section.equals("RENTALS") && rentalCount < MAX_RENTALS && p.length == 9) {
                    for (int j = 0; j < 9; j++) rentals[rentalCount][j] = p[j];
                    rentalCount++;
                } else if (section.equals("RESERVATIONS") && reservationCount < MAX_RENTALS && p.length == 9) {
                    for (int j = 0; j < 9; j++) reservations[reservationCount][j] = p[j];
                    reservationCount++;
                }
            }
        } catch (IOException ignored) { }
    }

    /** Writes ADMIN or STAFF credential records to their dedicated file. */
    static void saveCredentialFile(String filename, String role) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < userCount; i++) {
                if (users[i][2].equals(role))
                    pw.println(users[i][0] + "|" + users[i][1] + "|" + users[i][2]
                             + "|" + users[i][3] + "|" + users[i][4]);
            }
        } catch (IOException e) {
            System.out.println("  [!] Error saving " + filename + ": " + e.getMessage());
        }
    }

    /** Writes USER accounts (with favourites) to users.txt. */
    static void saveUsersFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (int i = 0; i < userCount; i++) {
                if (!users[i][2].equals("USER")) continue;
                StringBuilder sb = new StringBuilder();
                sb.append(users[i][0]).append("|").append(users[i][1]).append("|")
                  .append(users[i][2]).append("|").append(users[i][3]).append("|")
                  .append(users[i][4]);
                for (int j = 0; j < MAX_FAVORITES; j++)
                    sb.append("|").append(favorites[i][j] == null ? "" : favorites[i][j]);
                pw.println(sb);
            }
        } catch (IOException e) {
            System.out.println("  [!] Error saving users file: " + e.getMessage());
        }
    }

    /** Writes bikes, rentals, and reservations to data.txt. */
    static void saveDataFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            pw.println("[BIKES]");
            for (int i = 0; i < bikeCount; i++)
                pw.println(bikes[i][0] + "|" + bikes[i][1] + "|" + bikes[i][2] + "|"
                         + bikes[i][3] + "|" + bikes[i][4] + "|" + bikes[i][5] + "|"
                         + bikes[i][6] + "|" + bikes[i][7]);

            pw.println("[RENTALS]");
            for (int i = 0; i < rentalCount; i++)
                pw.println(rentals[i][0] + "|" + rentals[i][1] + "|" + rentals[i][2] + "|"
                         + rentals[i][3] + "|" + rentals[i][4] + "|" + rentals[i][5] + "|"
                         + rentals[i][6] + "|" + rentals[i][7] + "|" + rentals[i][8]);

            pw.println("[RESERVATIONS]");
            for (int i = 0; i < reservationCount; i++)
                pw.println(reservations[i][0] + "|" + reservations[i][1] + "|" + reservations[i][2] + "|"
                         + reservations[i][3] + "|" + reservations[i][4] + "|" + reservations[i][5] + "|"
                         + reservations[i][6] + "|" + reservations[i][7] + "|" + reservations[i][8]);
        } catch (IOException e) {
            System.out.println("  [!] Error saving data file: " + e.getMessage());
        }
    }

    // ════════════════════════ ADMIN MENU ═══════════════════════════════
    /** Full admin control panel — loops until the admin logs out. */
    static void adminMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n╔══════════════════════════════════╗");
            System.out.println("║       ADMIN CONTROL PANEL        ║");
            System.out.println("╠══════════════════════════════════╣");
            System.out.println("║  [1]  View All Motorbikes        ║");
            System.out.println("║  [2]  View Bikes by Type         ║");
            System.out.println("║  [3]  View Bikes by Brand        ║");
            System.out.println("║  [4]  Search Motorbikes          ║");
            System.out.println("║  [5]  Add Motorbike              ║");
            System.out.println("║  [6]  Remove Motorbike           ║");
            System.out.println("║  [7]  Update Bike Stock          ║");
            System.out.println("║  [8]  View Out-of-Stock Bikes    ║");
            System.out.println("║  [9]  View All Rental Records    ║");
            System.out.println("║  [10] View All Reservations      ║");
            System.out.println("║  [11] View All Accounts          ║");
            System.out.println("║  [12] Add Staff Account          ║");
            System.out.println("║  [0]  Logout                     ║");
            System.out.println("╚══════════════════════════════════╝");
            System.out.print("  Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":  viewAllBikes();           break;
                case "2":  viewBikesByType();        break;
                case "3":  viewBikesByBrand();       break;
                case "4":  searchMenu();             break;
                case "5":  addMotorbike();           break;
                case "6":  removeMotorbike();        break;
                case "7":  updateBikeStock();        break;
                case "8":  viewOutOfStockBikes();    break;
                case "9":  viewAllRentalRecords();   break;
                case "10": viewAllReservations();    break;
                case "11": viewAllAccounts();        break;
                case "12": addStaffAccount();        break;
                case "0":  logoutSession(); active = false; break;
                default:   System.out.println("  [!] Invalid choice.");
            }
        }
    }

    // ════════════════════════ STAFF MENU ═══════════════════════════════
    /** Staff control panel — loops until the staff member logs out. */
    static void staffMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║         STAFF CONTROL PANEL          ║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  [1] View Available Motorbikes       ║");
            System.out.println("║  [2] View Bikes by Type              ║");
            System.out.println("║  [3] View Bikes by Brand             ║");
            System.out.println("║  [4] Search Motorbikes               ║");
            System.out.println("║  [5] View Pending Reservations       ║");
            System.out.println("║  [6] Confirm Reservation & Payment   ║");
            System.out.println("║  [7] Process Return                  ║");
            System.out.println("║  [8] View All Rental Records         ║");
            System.out.println("║  [0] Logout                          ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("  Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": viewAvailableBikes();        break;
                case "2": viewBikesByType();           break;
                case "3": viewBikesByBrand();          break;
                case "4": searchMenu();                break;
                case "5": viewPendingReservations();   break;
                case "6": confirmReservationPayment(); break;
                case "7": processReturn();             break;
                case "8": viewAllRentalRecords();      break;
                case "0": logoutSession(); active = false; break;
                default:  System.out.println("  [!] Invalid choice.");
            }
        }
    }

    // ════════════════════════ USER MENU ════════════════════════════════
    /** Customer portal — loops until the customer logs out. */
    static void userMenu() {
        boolean active = true;
        while (active) {
            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║          CUSTOMER PORTAL             ║");
            System.out.println("║  Logged in as: " + padRight(currentUser, 22) + "║");
            System.out.println("╠══════════════════════════════════════╣");
            System.out.println("║  [1] View Available Motorbikes       ║");
            System.out.println("║  [2] View Bikes by Type              ║");
            System.out.println("║  [3] View Bikes by Brand             ║");
            System.out.println("║  [4] Search Motorbikes               ║");
            System.out.println("║  [5] Reserve a Motorbike             ║");
            System.out.println("║  [6] View My Reservations            ║");
            System.out.println("║  [7] View My Rental Status           ║");
            System.out.println("║  [8] My Favourites                   ║");
            System.out.println("║  [0] Logout                          ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.print("  Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1": viewAvailableBikes();  break;
                case "2": viewBikesByType();     break;
                case "3": viewBikesByBrand();    break;
                case "4": searchMenu();          break;
                case "5": reserveBike();         break;
                case "6": viewMyReservations();  break;
                case "7": viewMyRentalStatus();  break;
                case "8": manageFavourites();    break;
                case "0": logoutSession(); active = false; break;
                default:  System.out.println("  [!] Invalid choice.");
            }
        }
    }

    // ════════════════════ REGISTER (USER ONLY) ═════════════════════════
    /**
     * Registers a new USER account from the main menu.
     * Validates email and mobile with regex before saving.
     */
    static void registerUser() {
        System.out.println("\n  ─── USER REGISTRATION ───");

        if (userCount >= MAX_USERS) {
            System.out.println("  [!] System user capacity reached. Contact admin.");
            return;
        }

        String username;
        do {
            System.out.print("  Username (no spaces): ");
            username = sc.nextLine().trim();
            if (username.equals("0")) { 
                System.out.println("  Registration cancelled."); 
                return; 
            }
            // ... existing validation checks ...
            System.out.println("  You entered: " + username);
            System.out.print("  Is this correct? (Y/N): ");
            } while (!sc.nextLine().trim().equalsIgnoreCase("Y"));
        
        for (int i = 0; i < userCount; i++) {
            if (users[i][0].equalsIgnoreCase(username)) {
                System.out.println("  [!] Username already taken.");
                return;
            }
        }

        String password;
        do {
            printPasswordRules();
            System.out.print("  Password: ");
            password = sc.nextLine().trim();
            if (password.equals("0")) { System.out.println("  Registration cancelled."); return; }
            if (!isPasswordStrong(password)) {
                System.out.println("  [!] Password too weak. Try again.");
                System.out.println("  [!] Press Enter to Continue.");
                sc.nextLine();
                continue;
            }
            System.out.print("  Re-enter password: ");
            String confirm = sc.nextLine().trim();
            if (!password.equals(confirm)) {
                System.out.println("  [!] Passwords do not match. Try again.");
                password = ""; // force loop to retry
                continue;
            }
            System.out.print("  Password accepted. Keep this password? (Y/N): ");
        } while (!sc.nextLine().trim().equalsIgnoreCase("Y"));

        // ── Regex 1: Email validation ──────────────────────────────────
        System.out.print("  Email address       : ");
        String email = sc.nextLine().trim();
        if (!validateInput(email, "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            System.out.println("  [!] Invalid email format (e.g., user@example.com).");
            return;
        }

        // ── Regex 2: PH mobile number validation (09XXXXXXXXX) ─────────
        System.out.print("  Mobile (09XXXXXXXXX): ");
        String mobile = sc.nextLine().trim();
        if (!validateInput(mobile, "^09\\d{9}$")) {
            System.out.println("  [!] Invalid mobile. Format must be 09XXXXXXXXX (11 digits).");
            return;
        }

        System.out.println("  [✓] Registration successful! You may now login.");
        saveFiles();
    }

    // ════════════════════════ VIEW BIKES ═══════════════════════════════
    /** Displays every bike in the system (admin use). */
    static void viewAllBikes() {
        System.out.println("\n  ─── ALL MOTORBIKES ───");
        printBikeTableHeader();
        if (bikeCount == 0) { System.out.println("  (No bikes in system.)"); return; }
        for (int i = 0; i < bikeCount; i++) printBikeRow(i);
    }

    /** Displays only bikes with stock > 0. */
    static void viewAvailableBikes() {
        System.out.println("\n  ─── AVAILABLE MOTORBIKES ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            if (Integer.parseInt(bikes[i][6]) > 0) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (No bikes currently available.)");
    }

    static void printBikeTableHeader() {
        System.out.printf("  %-12s %-12s %-16s %-12s %-12s %-6s %-11s %-6s%n",
            "Bike ID", "Brand", "Model", "Type", "Color", "CC", "Daily(PHP)", "Stock");
        System.out.println("  " + "─".repeat(93));
    }

    static void printBikeRow(int i) {
        System.out.printf("  %-12s %-12s %-16s %-12s %-12s %-6s %-11s %-6s%n",
            bikes[i][7], bikes[i][0], bikes[i][1], bikes[i][2],
            bikes[i][3], bikes[i][4], bikes[i][5], bikes[i][6]);
    }

    // ════════════════════ VIEW BY TYPE / BRAND ═════════════════════════
    /**
     * Shows a numbered type list, then lists all bikes of the chosen type.
     */
    static void viewBikesByType() {
        String[] types = {
            "Standard", "Cruiser", "Chopper", "Touring", "Motocross",
            "Sport Bike", "Trail", "Scooter", "Off-road Bike",
            "Underbone", "Moped", "Tricycle", "ATV"
        };
        System.out.println("\n  ─── MOTORBIKE TYPES ───");
        for (int i = 0; i < types.length; i++)
            System.out.println("  [" + (i + 1) + "] " + types[i]);

        System.out.print("  Select type number: ");
        String input = sc.nextLine().trim();
        int idx;
        try { idx = Integer.parseInt(input) - 1; }
        catch (NumberFormatException e) { System.out.println("  [!] Invalid input."); return; }
        if (idx < 0 || idx >= types.length) { System.out.println("  [!] Out of range."); return; }

        System.out.println("\n  ─── " + types[idx].toUpperCase() + " BIKES ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            if (bikes[i][2].equalsIgnoreCase(types[idx])) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (No bikes found for type: " + types[idx] + ")");
    }

    /** Prompts for a brand name and lists all matching bikes. */
    static void viewBikesByBrand() {
        System.out.print("\n  Enter brand name: ");
        String brand = sc.nextLine().trim();
        System.out.println("\n  ─── " + brand.toUpperCase() + " BIKES ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            if (bikes[i][0].equalsIgnoreCase(brand)) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (No bikes found for brand: " + brand + ")");
    }

    // ═════════════════════════ SEARCH MENU ════════════════════════════
    /** Sub-menu routing to one of three search methods. */
    static void searchMenu() {
        System.out.println("\n  ─── SEARCH MOTORBIKES ───");
        System.out.println("  [1] Search by Bike Name / Model");
        System.out.println("  [2] Search by Daily Price Range");
        System.out.println("  [3] Search by Engine CC Range");
        System.out.print("  Enter choice: ");
        String choice = sc.nextLine().trim();
        switch (choice) {
            case "1": searchByModel();    break;
            case "2": searchByPrice();    break;
            case "3": searchByEngineCC(); break;
            default:  System.out.println("  [!] Invalid choice.");
        }
    }

    /** Searches bikes whose brand or model contains the keyword. */
    static void searchByModel() {
        System.out.print("  Enter keyword: ");
        String kw = sc.nextLine().trim().toLowerCase();
        System.out.println("\n  ─── SEARCH RESULTS: \"" + kw + "\" ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            if (bikes[i][0].toLowerCase().contains(kw) || bikes[i][1].toLowerCase().contains(kw)) {
                printBikeRow(i);
                found = true;
            }
        }
        if (!found) System.out.println("  (No bikes matched \"" + kw + "\".)");
    }

    /** Lists bikes whose daily rate is within the entered range. */
    static void searchByPrice() {
        System.out.print("  Min daily rate (PHP): ");
        String minStr = sc.nextLine().trim();
        System.out.print("  Max daily rate (PHP): ");
        String maxStr = sc.nextLine().trim();
        if (!validateInput(minStr, "^\\d+$") || !validateInput(maxStr, "^\\d+$")) {
            System.out.println("  [!] Invalid price. Use whole numbers only.");
            return;
        }
        int min = Integer.parseInt(minStr), max = Integer.parseInt(maxStr);
        System.out.println("\n  ─── BIKES: PHP " + min + " – " + max + "/day ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            int rate = Integer.parseInt(bikes[i][5]);
            if (rate >= min && rate <= max) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (No bikes in that price range.)");
    }

    /** Lists bikes whose engine displacement is within the entered CC range. */
    static void searchByEngineCC() {
        System.out.print("  Min engine CC: ");
        String minStr = sc.nextLine().trim();
        System.out.print("  Max engine CC: ");
        String maxStr = sc.nextLine().trim();
        if (!validateInput(minStr, "^\\d+$") || !validateInput(maxStr, "^\\d+$")) {
            System.out.println("  [!] Invalid CC. Use whole numbers only.");
            return;
        }
        int min = Integer.parseInt(minStr), max = Integer.parseInt(maxStr);
        System.out.println("\n  ─── BIKES: " + min + "cc – " + max + "cc ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            int cc = Integer.parseInt(bikes[i][4]);
            if (cc >= min && cc <= max) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (No bikes in that CC range.)");
    }

    // ════════════════════ ADMIN: ADD MOTORBIKE ═════════════════════════
    /**
     * Collects bike details from admin, validates numeric fields,
     * generates a bike ID, and appends to the bikes array.
     */
    static void addMotorbike() {
        System.out.println("\n  ─── ADD NEW MOTORBIKE ───");
        if (bikeCount >= MAX_BIKES) {
            System.out.println("  [!] Bike capacity full (" + MAX_BIKES + " max).");
            return;
        }

        System.out.print("  Brand            : "); String brand  = sc.nextLine().trim();
        System.out.print("  Model            : "); String model  = sc.nextLine().trim();

        System.out.println("  Types: Standard | Cruiser | Chopper | Touring | Motocross");
        System.out.println("         Sport Bike | Trail | Scooter | Off-road Bike");
        System.out.println("         Underbone | Moped | Tricycle | ATV");
        System.out.print("  Type             : "); String type   = sc.nextLine().trim();
        System.out.print("  Color            : "); String color  = sc.nextLine().trim();

        System.out.print("  Engine CC        : "); String cc    = sc.nextLine().trim();
        if (!validateInput(cc, "^\\d+$")) { System.out.println("  [!] CC must be a whole number."); return; }

        System.out.print("  Daily Rate (PHP) : "); String rate  = sc.nextLine().trim();
        if (!validateInput(rate, "^\\d+$")) { System.out.println("  [!] Rate must be a whole number."); return; }

        System.out.print("  Initial Stock    : "); String stock = sc.nextLine().trim();
        if (!validateInput(stock, "^\\d+$")) { System.out.println("  [!] Stock must be a whole number."); return; }

        bikes[bikeCount][0] = brand;
        bikes[bikeCount][1] = model;
        bikes[bikeCount][2] = type;
        bikes[bikeCount][3] = color;
        bikes[bikeCount][4] = cc;
        bikes[bikeCount][5] = rate;
        bikes[bikeCount][6] = stock;
        bikes[bikeCount][7] = generateBikeId(bikeCount);
        String newId = bikes[bikeCount][7];
        bikeCount++;

        System.out.println("  [✓] Motorbike added! ID: " + newId);
        saveFiles();
    }

    // ════════════════════ ADMIN: REMOVE MOTORBIKE ══════════════════════
    /**
     * Validates Bike ID format with regex, confirms with admin, then
     * removes the entry by shifting the array left.
     */
    static void removeMotorbike() {
        System.out.println("\n  ─── REMOVE MOTORBIKE ───");
        viewAllBikes();

        System.out.print("\n  Enter Bike ID to remove (e.g. MV-0001-01): ");
        String bikeId = sc.nextLine().trim();

        // ── Regex 3: Bike ID format (MV-XXXX-XX) ──────────────────────
        if (!validateInput(bikeId, "^MV-\\d{4}-\\d{2}$")) {
            System.out.println("  [!] Invalid Bike ID format. Must be MV-XXXX-XX.");
            return;
        }

        int idx = findBikeById(bikeId);
        if (idx == -1) { System.out.println("  [!] Bike not found."); return; }

        System.out.print("  Confirm removal of " + bikes[idx][0] + " " + bikes[idx][1] + "? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("  Removal cancelled.");
            return;
        }

        for (int i = idx; i < bikeCount - 1; i++)
            for (int j = 0; j < 8; j++) bikes[i][j] = bikes[i + 1][j];
        bikeCount--;

        System.out.println("  [✓] Motorbike removed successfully.");
        saveFiles();
    }

    // ════════════════════ ADMIN: UPDATE STOCK ══════════════════════════
    /** Sets a new stock quantity for any bike. */
    static void updateBikeStock() {
        System.out.println("\n  ─── UPDATE BIKE STOCK ───");
        viewAllBikes();

        System.out.print("\n  Enter Bike ID: ");
        String bikeId = sc.nextLine().trim();
        int idx = findBikeById(bikeId);
        if (idx == -1) { System.out.println("  [!] Bike not found."); return; }

        System.out.println("  Current stock : " + bikes[idx][6]);
        System.out.print("  New stock amount: ");
        String newStock = sc.nextLine().trim();
        if (!validateInput(newStock, "^\\d+$")) { System.out.println("  [!] Must be a whole number."); return; }

        bikes[idx][6] = newStock;
        System.out.println("  [✓] Stock updated to " + newStock + ".");
        if (Integer.parseInt(newStock) == 0)
            System.out.println("  [!] WARNING: " + bikes[idx][0] + " " + bikes[idx][1] + " is now OUT OF STOCK!");
        saveFiles();
    }

    // ════════════════════ ADMIN: OUT-OF-STOCK ══════════════════════════
    /** Lists every bike whose stock is zero. */
    static void viewOutOfStockBikes() {
        System.out.println("\n  ─── OUT-OF-STOCK MOTORBIKES ───");
        printBikeTableHeader();
        boolean found = false;
        for (int i = 0; i < bikeCount; i++) {
            if (Integer.parseInt(bikes[i][6]) == 0) { printBikeRow(i); found = true; }
        }
        if (!found) System.out.println("  (All bikes are currently in stock.)");
    }

    // ════════════════════ ADMIN: RENTAL RECORDS ════════════════════════
    /** Prints the complete rental log table. */
    static void viewAllRentalRecords() {
        System.out.println("\n  ─── ALL RENTAL RECORDS ───");
        printRentalTableHeader();
        if (rentalCount == 0) { System.out.println("  (No rental records yet.)"); return; }
        for (int i = 0; i < rentalCount; i++) printRentalRow(i);
    }

    static void printRentalTableHeader() {
        System.out.printf("  %-10s %-14s %-12s %-12s %-12s %-10s %-14s %-10s%n",
            "Rental ID", "Customer", "Bike ID", "Start", "End", "Status", "Payment", "Total(PHP)");
        System.out.println("  " + "─".repeat(98));
    }

    static void printRentalRow(int i) {
        System.out.printf("  %-10s %-14s %-12s %-12s %-12s %-10s %-14s %-10s%n",
            rentals[i][0], rentals[i][1], rentals[i][2], rentals[i][3],
            rentals[i][4], rentals[i][5], rentals[i][6], rentals[i][7]);
    }

    // ════════════════════ ADMIN: ALL RESERVATIONS ══════════════════════
    /** Shows every reservation record (any status). */
    static void viewAllReservations() {
        System.out.println("\n  ─── ALL RESERVATION RECORDS ───");
        printReservationTableHeader();
        boolean found = false;
        for (int i = 0; i < reservationCount; i++) { printReservationRow(i); found = true; }
        if (!found) System.out.println("  (No reservations yet.)");
    }

    static void printReservationTableHeader() {
        System.out.printf("  %-12s %-14s %-12s %-12s %-12s %-12s %-14s %-10s%n",
            "Reserve ID", "Customer", "Bike ID", "Start", "End", "Status", "Payment", "Total(PHP)");
        System.out.println("  " + "─".repeat(102));
    }

    static void printReservationRow(int i) {
        System.out.printf("  %-12s %-14s %-12s %-12s %-12s %-12s %-14s %-10s%n",
            reservations[i][0], reservations[i][1], reservations[i][2],
            reservations[i][3], reservations[i][4], reservations[i][5],
            reservations[i][6], reservations[i][7]);
    }

    // ════════════════════ ADMIN: ALL ACCOUNTS ══════════════════════════
    /** Displays every user/staff/admin account (passwords never shown). */
    static void viewAllAccounts() {
        System.out.println("\n  ─── ALL SYSTEM ACCOUNTS ───");
        System.out.printf("  %-16s %-8s %-30s %-14s%n", "Username", "Role", "Email", "Mobile");
        System.out.println("  " + "─".repeat(72));
        for (int i = 0; i < userCount; i++)
            System.out.printf("  %-16s %-8s %-30s %-14s%n",
                users[i][0], users[i][2], users[i][3], users[i][4]);
    }

    // ════════════════════ ADMIN: ADD STAFF ACCOUNT ═════════════════════
    /**
     * Creates a new STAFF account with regex-validated email and mobile.
     */
    static void addStaffAccount() {
        System.out.println("\n  ─── ADD STAFF ACCOUNT ───");
        if (userCount >= MAX_USERS) { System.out.println("  [!] User capacity full."); return; }

        System.out.print("  Staff username          : ");
        String username = sc.nextLine().trim();
        for (int i = 0; i < userCount; i++) {
            if (users[i][0].equalsIgnoreCase(username)) {
                System.out.println("  [!] Username already taken.");
                return;
            }
        }
        
        printPasswordRules();
        System.out.println("  Password (min 8 chars): ");
        String password = sc.nextLine().trim();
        if (!isPasswordStrong(password)) {
            printPasswordRules();
            System.out.println("  [!] Password does not meet requirements.");
            return;
        }

        // ── Regex: Email ────────────────────────────────────────────────
        System.out.print("  Email                   : ");
        String email = sc.nextLine().trim();
        if (!validateInput(email, "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            System.out.println("  [!] Invalid email format."); return;
        }

        // ── Regex: Mobile ───────────────────────────────────────────────
        System.out.print("  Mobile (09XXXXXXXXX)    : ");
        String mobile = sc.nextLine().trim();
        if (!validateInput(mobile, "^09\\d{9}$")) {
            System.out.println("  [!] Invalid mobile. Must be 09XXXXXXXXX."); return;
        }

        System.out.println("  [✓] Staff account created: " + username);
        saveFiles();
    }

    // ══════════ STAFF: VIEW PENDING RESERVATIONS ═══════════════════════
    /** Lists only reservations that are awaiting staff approval (PENDING). */
    static void viewPendingReservations() {
        System.out.println("\n  ─── PENDING RESERVATIONS ───");
        printReservationTableHeader();
        boolean found = false;
        for (int i = 0; i < reservationCount; i++) {
            if (reservations[i][5].equals("PENDING")) { printReservationRow(i); found = true; }
        }
        if (!found) System.out.println("  (No pending reservations.)");
    }

    // ══════════ STAFF: CONFIRM RESERVATION & PROCESS PAYMENT ══════════
    /**
     * Staff selects a PENDING reservation, chooses a payment method,
     * marks it CONFIRMED, creates an ACTIVE rental record, decrements
     * bike stock, and prints a formatted receipt.
     */
    static void confirmReservationPayment() {
        System.out.println("\n  ─── CONFIRM RESERVATION & PROCESS PAYMENT ───");

        // Show pending reservations first
        viewPendingReservations();

        System.out.print("\n  Enter Reserve ID to confirm (e.g. RSV-0001): ");
        String rsvId = sc.nextLine().trim();

        // Find the reservation
        int rsvIdx = -1;
        for (int i = 0; i < reservationCount; i++) {
            if (reservations[i][0].equals(rsvId) && reservations[i][5].equals("PENDING")) {
                rsvIdx = i;
                break;
            }
        }
        if (rsvIdx == -1) {
            System.out.println("  [!] Pending reservation not found.");
            return;
        }

        // Verify the bike is still available
        int bikeIdx = findBikeById(reservations[rsvIdx][2]);
        if (bikeIdx == -1) {
            System.out.println("  [!] Associated bike no longer exists.");
            return;
        }
        if (Integer.parseInt(bikes[bikeIdx][6]) <= 0) {
            System.out.println("  [!] That bike is currently out of stock. Cannot confirm.");
            return;
        }

        // Show reservation details
        System.out.println("\n  ── Reservation Details ──────────────────────");
        System.out.println("  Reserve ID  : " + reservations[rsvIdx][0]);
        System.out.println("  Customer    : " + reservations[rsvIdx][1]);
        System.out.println("  Bike        : " + bikes[bikeIdx][0] + " " + bikes[bikeIdx][1]
                           + "  [" + bikes[bikeIdx][7] + "]");
        System.out.println("  Type        : " + bikes[bikeIdx][2]);
        System.out.println("  Start Date  : " + reservations[rsvIdx][3]);
        System.out.println("  End Date    : " + reservations[rsvIdx][4]);
        System.out.println("  Duration    : " + reservations[rsvIdx][8] + " day/s");
        System.out.println("  Total Cost  : PHP " + reservations[rsvIdx][7]);
        System.out.println("  ─────────────────────────────────────────────");

        // ── Select payment method ──────────────────────────────────────
        String paymentMethod = selectPaymentMethod();
        if (paymentMethod == null) return; // Cancelled

        System.out.print("\n  Confirm reservation and process payment? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("  Confirmation cancelled.");
            return;
        }

        // ── Update reservation status ──────────────────────────────────
        reservations[rsvIdx][5] = "CONFIRMED";
        reservations[rsvIdx][6] = paymentMethod;

        // ── Create ACTIVE rental record ────────────────────────────────
        if (rentalCount < MAX_RENTALS) {
            String rentalId = "RNT-" + String.format("%04d", rentalCount + 1);
            rentals[rentalCount][0] = rentalId;
            rentals[rentalCount][1] = reservations[rsvIdx][1];   // customer
            rentals[rentalCount][2] = reservations[rsvIdx][2];   // bikeId
            rentals[rentalCount][3] = reservations[rsvIdx][3];   // startDate
            rentals[rentalCount][4] = reservations[rsvIdx][4];   // endDate
            rentals[rentalCount][5] = "ACTIVE";
            rentals[rentalCount][6] = paymentMethod;
            rentals[rentalCount][7] = reservations[rsvIdx][7];   // totalCost
            rentals[rentalCount][8] = reservations[rsvIdx][8];   // days
            rentalCount++;

            // ── Decrement bike stock ───────────────────────────────────
            int newStock = Integer.parseInt(bikes[bikeIdx][6]) - 1;
            bikes[bikeIdx][6] = String.valueOf(newStock);

            // ── Print receipt ──────────────────────────────────────────
            printReceipt(rentalId, reservations[rsvIdx], bikes[bikeIdx], paymentMethod, currentUser);

            if (newStock == 0)
                System.out.println("\n  [!] ALERT: " + bikes[bikeIdx][0] + " "
                                   + bikes[bikeIdx][1] + " is now OUT OF STOCK!");
        } else {
            System.out.println("  [!] Rental log full. Could not create rental record.");
        }

        saveFiles();
    }

    // ════════════════════ PAYMENT METHOD SELECTOR ══════════════════════
    /**
     * Displays the payment method menu and returns the selected method name.
     * Returns null if the user cancels.
     */
    static String selectPaymentMethod() {
        System.out.println("\n  ─── SELECT PAYMENT METHOD ───");
        System.out.println("  [1] GCash");
        System.out.println("  [2] PayMaya");
        System.out.println("  [3] PayPal");
        System.out.println("  [4] InstaPay");
        System.out.println("  [5] MariBank");
        System.out.println("  [6] Apple Pay");
        System.out.println("  [7] Cash");
        System.out.println("  [0] Cancel");
        System.out.print("  Enter choice: ");
        String choice = sc.nextLine().trim();

        switch (choice) {
            case "1": return "GCash";
            case "2": return "PayMaya";
            case "3": return "PayPal";
            case "4": return "InstaPay";
            case "5": return "MariBank";
            case "6": return "Apple Pay";
            case "7": return "Cash";
            case "0":
                System.out.println("  Payment cancelled.");
                return null;
            default:
                System.out.println("  [!] Invalid payment method. Defaulting to Cash.");
                return "Cash";
        }
    }

    // ════════════════════════ RECEIPT PRINTER ══════════════════════════
    /**
     * Prints a formatted rental receipt to the console after payment is
     * processed. Called exclusively by confirmReservationPayment().
     *
     * @param rentalId    the newly created rental ID
     * @param rsv         the reservation row (9-element array)
     * @param bike        the bike row (8-element array)
     * @param payment     payment method string
     * @param staffName   name of the staff who processed the transaction
     */
    static void printReceipt(String rentalId, String[] rsv,
                              String[] bike, String payment, String staffName) {
        System.out.println("\n  ╔═══════════════════════════════════════════════╗");
        System.out.println("  ║              M O T O V A U L T                ║");
        System.out.println("  ║         OFFICIAL RENTAL RECEIPT               ║");
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.printf( "  ║  Rental ID   : %-31s║%n", rentalId);
        System.out.printf( "  ║  Reserve ID  : %-31s║%n", rsv[0]);
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.println("  ║  CUSTOMER DETAILS                             ║");
        System.out.printf( "  ║  Name        : %-31s║%n", rsv[1]);
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.println("  ║  MOTORBIKE DETAILS                            ║");
        System.out.printf( "  ║  Bike ID     : %-31s║%n", bike[7]);
        System.out.printf( "  ║  Brand       : %-31s║%n", bike[0]);
        System.out.printf( "  ║  Model       : %-31s║%n", bike[1]);
        System.out.printf( "  ║  Type        : %-31s║%n", bike[2]);
        System.out.printf( "  ║  Color       : %-31s║%n", bike[3]);
        System.out.printf( "  ║  Engine CC   : %-31s║%n", bike[4] + "cc");
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.println("  ║  RENTAL PERIOD                                ║");
        System.out.printf( "  ║  Start Date  : %-31s║%n", rsv[3]);
        System.out.printf( "  ║  End Date    : %-31s║%n", rsv[4]);
        System.out.printf( "  ║  Duration    : %-31s║%n", rsv[8] + " day/s");
        System.out.printf( "  ║  Daily Rate  : %-31s║%n", "PHP " + bike[5]);
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.println("  ║  PAYMENT SUMMARY                              ║");
        System.out.printf( "  ║  Method      : %-31s║%n", payment);
        System.out.printf( "  ║  Status      : %-31s║%n", "PAID");
        System.out.printf( "  ║  TOTAL COST  : %-31s║%n", "PHP " + rsv[7]);
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.printf( "  ║  Processed by: %-31s║%n", staffName);
        System.out.println("  ╠═══════════════════════════════════════════════╣");
        System.out.println("  ║   Thank you for choosing MotoVault!           ║");
        System.out.println("  ║   Please ride safely and return on time.      ║");
        System.out.println("  ╚═══════════════════════════════════════════════╝");
    }

    // ════════════════════ STAFF: PROCESS RETURN ════════════════════════
    /**
     * Marks an ACTIVE rental as RETURNED and restores the bike's stock by 1.
     */
    static void processReturn() {
        System.out.println("\n  ─── PROCESS MOTORBIKE RETURN ───");
        System.out.print("  Enter Rental ID (e.g. RNT-0001): ");
        String rentalId = sc.nextLine().trim();

        int idx = -1;
        for (int i = 0; i < rentalCount; i++) {
            if (rentals[i][0].equals(rentalId) && rentals[i][5].equals("ACTIVE")) {
                idx = i;
                break;
            }
        }
        if (idx == -1) { System.out.println("  [!] Active rental not found."); return; }

        System.out.println("  Customer    : " + rentals[idx][1]);
        System.out.println("  Bike ID     : " + rentals[idx][2]);
        System.out.println("  Period      : " + rentals[idx][3] + " → " + rentals[idx][4]);
        System.out.println("  Payment     : " + rentals[idx][6]);
        System.out.println("  Total Cost  : PHP " + rentals[idx][7]);
        System.out.print("  Confirm return? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("  Return cancelled.");
            return;
        }

        rentals[idx][5] = "RETURNED";

        int bikeIdx = findBikeById(rentals[idx][2]);
        if (bikeIdx != -1)
            bikes[bikeIdx][6] = String.valueOf(Integer.parseInt(bikes[bikeIdx][6]) + 1);

        System.out.println("  [✓] Motorbike returned successfully. Stock restored.");
        saveFiles();
    }

    // ════════════════════ USER: RESERVE A BIKE ═════════════════════════
    /**
     * Customer reservation:
     * – selects a bike and a start/end date
     * – cost is calculated and shown before confirmation
     * – reservation is saved as PENDING (awaiting staff approval + payment)
     * – no rental is created and no stock is decremented at this stage
     */
    static void reserveBike() {
        System.out.println("\n  ─── RESERVE A MOTORBIKE ───");
        System.out.println("  Note: Reservations are subject to staff approval and payment.");

        if (reservationCount >= MAX_RENTALS) {
            System.out.println("  [!] Reservation log is full. Please contact staff.");
            return;
        }

        viewAvailableBikes();

        System.out.print("\n  Bike ID to reserve: ");
        String bikeId = sc.nextLine().trim();
        int bikeIdx = findBikeById(bikeId);
        if (bikeIdx == -1) { System.out.println("  [!] Bike not found."); return; }
        if (Integer.parseInt(bikes[bikeIdx][6]) <= 0) {
            System.out.println("  [!] That bike is currently out of stock.");
            return;
        }

        // ── Regex 4: Start date (DD/MM/YYYY) ───────────────────────────
        System.out.print("  Start date (DD/MM/YYYY): ");
        String startDate = sc.nextLine().trim();
        if (!validateInput(startDate, "^\\d{2}/\\d{2}/\\d{4}$")) {
            System.out.println("  [!] Invalid date format. Use DD/MM/YYYY.");
            return;
        }

        // ── Regex: End date (DD/MM/YYYY) ────────────────────────────────
        System.out.print("  End date   (DD/MM/YYYY): ");
        String endDate = sc.nextLine().trim();
        if (!validateInput(endDate, "^\\d{2}/\\d{2}/\\d{4}$")) {
            System.out.println("  [!] Invalid date format. Use DD/MM/YYYY.");
            return;
        }

        int days = calculateDays(startDate, endDate);
        if (days <= 0) {
            System.out.println("  [!] End date must be after start date.");
            return;
        }

        int dailyRate  = Integer.parseInt(bikes[bikeIdx][5]);
        int totalCost  = days * dailyRate;

        // Show reservation summary before confirming
        System.out.println("\n  ── Reservation Summary ─────────────────────");
        System.out.println("  Bike        : " + bikes[bikeIdx][0] + " " + bikes[bikeIdx][1]);
        System.out.println("  Type        : " + bikes[bikeIdx][2]);
        System.out.println("  Color       : " + bikes[bikeIdx][3]);
        System.out.println("  Engine CC   : " + bikes[bikeIdx][4] + "cc");
        System.out.println("  Start Date  : " + startDate);
        System.out.println("  End Date    : " + endDate);
        System.out.println("  Duration    : " + days + " day/s");
        System.out.println("  Daily Rate  : PHP " + dailyRate);
        System.out.println("  Total Cost  : PHP " + totalCost);
        System.out.println("  Status      : PENDING (awaiting staff approval)");
        System.out.println("  ─────────────────────────────────────────────");
        System.out.print("  Submit reservation? (Y/N): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("  Reservation cancelled.");
            return;
        }

        // Save reservation with PENDING status; no payment method yet
        String rsvId = "RSV-" + String.format("%04d", reservationCount + 1);
        reservations[reservationCount][0] = rsvId;
        reservations[reservationCount][1] = currentUser;
        reservations[reservationCount][2] = bikeId;
        reservations[reservationCount][3] = startDate;
        reservations[reservationCount][4] = endDate;
        reservations[reservationCount][5] = "PENDING";
        reservations[reservationCount][6] = "";                        // payment method (set by staff)
        reservations[reservationCount][7] = String.valueOf(totalCost); // total cost
        reservations[reservationCount][8] = String.valueOf(days);      // number of days
        reservationCount++;

        System.out.println("  [✓] Reservation submitted!");
        System.out.println("  Reserve ID  : " + rsvId);
        System.out.println("  Please wait for staff to confirm your reservation and collect payment.");
        saveFiles();
    }

    // ════════════════════ USER: VIEW MY RESERVATIONS ═══════════════════
    /** Shows only the logged-in customer's reservations. */
    static void viewMyReservations() {
        System.out.println("\n  ─── MY RESERVATIONS ───");
        System.out.printf("  %-12s %-12s %-12s %-12s %-12s %-14s %-10s%n",
            "Reserve ID", "Bike ID", "Start", "End", "Status", "Payment", "Total(PHP)");
        System.out.println("  " + "─".repeat(88));
        boolean found = false;
        for (int i = 0; i < reservationCount; i++) {
            if (reservations[i][1].equals(currentUser)) {
                System.out.printf("  %-12s %-12s %-12s %-12s %-12s %-14s %-10s%n",
                    reservations[i][0], reservations[i][2], reservations[i][3],
                    reservations[i][4], reservations[i][5], reservations[i][6],
                    reservations[i][7]);
                found = true;
            }
        }
        if (!found) System.out.println("  (No reservations on record.)");
    }

    // ════════════════════ USER: VIEW MY RENTAL STATUS ══════════════════
    /** Shows only the logged-in customer's confirmed/active rental records. */
    static void viewMyRentalStatus() {
        System.out.println("\n  ─── MY RENTAL STATUS ───");
        printRentalTableHeader();
        boolean found = false;
        for (int i = 0; i < rentalCount; i++) {
            if (rentals[i][1].equals(currentUser)) { printRentalRow(i); found = true; }
        }
        if (!found) System.out.println("  (No rental records for your account.)");
    }

    // ════════════════════ USER: FAVOURITES ═════════════════════════════
    /** Favourites sub-menu: view, add, remove, or compare saved bikes. */
    static void manageFavourites() {
        boolean active = true;
        while (active) {
            System.out.println("\n  ─── MY FAVOURITES ───");
            displayFavourites();
            System.out.println("\n  [1] Add bike to favourites");
            System.out.println("  [2] Remove bike from favourites");
            System.out.println("  [3] Compare favourites");
            System.out.println("  [0] Back");
            System.out.print("  Choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": addToFavourites();      break;
                case "2": removeFromFavourites(); break;
                case "3": compareFavourites();    break;
                case "0": active = false;         break;
                default:  System.out.println("  [!] Invalid choice.");
            }
        }
    }

    /** Renders the current user's favourites list as a bike table. */
    static void displayFavourites() {
        boolean found = false;
        for (int j = 0; j < MAX_FAVORITES; j++) {
            String fav = favorites[currentUserIndex][j];
            if (fav != null && !fav.isEmpty()) {
                if (!found) printBikeTableHeader();
                int bikeIdx = findBikeById(fav);
                if (bikeIdx != -1) { printBikeRow(bikeIdx); found = true; }
            }
        }
        if (!found) System.out.println("  (Your favourites list is empty.)");
    }

    /** Saves a bike ID into the first free slot of the user's favourites. */
    static void addToFavourites() {
        viewAvailableBikes();
        System.out.print("\n  Bike ID to add to favourites: ");
        String bikeId = sc.nextLine().trim();

        int bikeIdx = findBikeById(bikeId);
        if (bikeIdx == -1) { System.out.println("  [!] Bike not found."); return; }

        for (int j = 0; j < MAX_FAVORITES; j++) {
            if (bikeId.equals(favorites[currentUserIndex][j])) {
                System.out.println("  [!] Already in favourites.");
                return;
            }
        }
        for (int j = 0; j < MAX_FAVORITES; j++) {
            if (favorites[currentUserIndex][j] == null || favorites[currentUserIndex][j].isEmpty()) {
                favorites[currentUserIndex][j] = bikeId;
                System.out.println("  [✓] Added: " + bikes[bikeIdx][0] + " " + bikes[bikeIdx][1]);
                saveFiles();
                return;
            }
        }
        System.out.println("  [!] Favourites full (max " + MAX_FAVORITES + "). Remove one first.");
    }

    /** Clears a bike ID from the user's favourites array. */
    static void removeFromFavourites() {
        displayFavourites();
        System.out.print("\n  Bike ID to remove: ");
        String bikeId = sc.nextLine().trim();
        for (int j = 0; j < MAX_FAVORITES; j++) {
            if (bikeId.equals(favorites[currentUserIndex][j])) {
                favorites[currentUserIndex][j] = "";
                System.out.println("  [✓] Removed from favourites.");
                saveFiles();
                return;
            }
        }
        System.out.println("  [!] Bike not found in favourites.");
    }

    /** Prints a side-by-side comparison table of all favourited bikes. */
    static void compareFavourites() {
        System.out.println("\n  ─── FAVOURITES COMPARISON ───");
        int[] favIdx = new int[MAX_FAVORITES];
        int count = 0;
        for (int j = 0; j < MAX_FAVORITES; j++) {
            String fav = favorites[currentUserIndex][j];
            if (fav != null && !fav.isEmpty()) {
                int idx = findBikeById(fav);
                if (idx != -1) favIdx[count++] = idx;
            }
        }
        if (count < 2) {
            System.out.println("  [!] Add at least 2 bikes to compare.");
            return;
        }
        String[] fieldNames = { "Brand", "Model", "Type", "Color", "Engine CC", "Daily Rate", "Stock", "Bike ID" };
        for (int f = 0; f < 8; f++) {
            System.out.printf("  %-14s", fieldNames[f]);
            for (int b = 0; b < count; b++) System.out.printf("%-20s", bikes[favIdx[b]][f]);
            System.out.println();
        }
    }

    // ════════════════════════ HELPER METHODS ═══════════════════════════

    /** Returns the array index of a bike by its ID string, or -1 if not found. */
    static int findBikeById(String bikeId) {
        for (int i = 0; i < bikeCount; i++)
            if (bikes[i][7] != null && bikes[i][7].equals(bikeId)) return i;
        return -1;
    }

    /**
     * Calculates the number of days between two DD/MM/YYYY dates
     * using a simplified day-count formula.
     *
     * @param start date string "DD/MM/YYYY"
     * @param end   date string "DD/MM/YYYY"
     * @return number of days (≥ 0), or 1 on parse failure
     */
    static int calculateDays(String start, String end) {
        try {
            String[] s = start.split("/");
            String[] e = end.split("/");
            int dd1 = Integer.parseInt(s[0]), mm1 = Integer.parseInt(s[1]), yy1 = Integer.parseInt(s[2]);
            int dd2 = Integer.parseInt(e[0]), mm2 = Integer.parseInt(e[1]), yy2 = Integer.parseInt(e[2]);
            int j1  = yy1 * 365 + mm1 * 30 + dd1;
            int j2  = yy2 * 365 + mm2 * 30 + dd2;
            return j2 - j1;
        } catch (Exception ex) {
            return 1;
        }
    }

    /** Clears session state (call on every logout). */
    static void logoutSession() {
        System.out.println("  [✓] " + currentUser + " logged out successfully.");
        currentUser      = "";
        currentRole      = "";
        currentUserIndex = -1;
    }

    /**
     * Right-pads or truncates a string to exactly n characters.
     * Used for box-drawing alignment in menus.
     */
    static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
    
    static boolean isPasswordStrong(String password) {
        if (password.length() < 8)                          return false;
        if (!password.matches(".*[A-Z].*"))                 return false;
        if (!password.matches(".*[a-z].*"))                 return false;
        if (!password.matches(".*[0-9].*"))                 return false;
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) return false;
        return true;
    }

    static void printPasswordRules() {
        System.out.println("  Password requirements:");
        System.out.println("    - Minimum 8 characters");
        System.out.println("    - At least one uppercase letter (A-Z)");
        System.out.println("    - At least one lowercase letter (a-z)");
        System.out.println("    - At least one number (0-9)");
        System.out.println("    - At least one special character (!@#$%^&* etc.)");
    }
}
