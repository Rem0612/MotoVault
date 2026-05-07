# 🏍️ MotoVault
### Motorcycle Inventory & Rental Tracker
> A console-based Java application for managing motorcycle inventory, customer reservations, staff approvals, and payment processing in a rental shop setting.

---

## 📋 Table of Contents
- [Requirements](#requirements)
- [Project Files](#project-files)
- [How to Compile](#how-to-compile)
- [How to Run](#how-to-run)
- [Sample Credentials](#sample-credentials)
- [System Flow](#system-flow)
- [Role Capabilities](#role-capabilities)
- [Payment Methods](#payment-methods)
- [Data Files](#data-files)
- [Notes](#notes)

---

## ✅ Requirements

| Requirement | Details |
|---|---|
| Java Version | JDK 8 or higher |
| IDE (optional) | IntelliJ IDEA, Eclipse, VS Code, NetBeans |
| Terminal | Any terminal / command prompt works |

---

## 📁 Project Files

```
MotoVault/
├── MotoVault.java      ← Main source file (compile and run this)
├── README.md           ← This file
│
│   (Auto-generated on first run)
├── admin.txt           ← Encrypted admin credentials
├── staff.txt           ← Encrypted staff credentials
├── users.txt           ← Encrypted user accounts + favourites
└── data.txt            ← Bikes, rentals, and reservations
```

> **Note:** The `.txt` data files are created automatically the first time you exit the program. You do not need to create them manually.

---

## ⚙️ How to Compile

### Using a Terminal / Command Prompt

**Step 1** — Navigate to the folder containing `MotoVault.java`:
```bash
cd path/to/your/folder
```

**Step 2** — Compile the file:
```bash
javac MotoVault.java
```

A successful compile produces no output and generates `MotoVault.class` in the same folder.

---

### Using an IDE

| IDE | Steps |
|---|---|
| **IntelliJ IDEA** | Open the file → Right-click → `Run 'MotoVault.main()'` |
| **Eclipse** | File → New Java Project → Add file → Run As → Java Application |
| **VS Code** | Install the *Extension Pack for Java* → Open file → Click ▶ Run |
| **NetBeans** | New Project → Java Application → Paste code → Run Project |

---

## ▶️ How to Run

After compiling, run with:
```bash
java MotoVault
```

You will be greeted with the **Main Menu**:

```
  ╔══════════════════════════════════════════╗
  ║         Welcome to  M O T O V A U L T    ║
  ║    Motorcycle Inventory & Rental System  ║
  ╚══════════════════════════════════════════╝

╔══════════════════════════════╗
║         MAIN  MENU           ║
╠══════════════════════════════╣
║  [1] Login                   ║
║  [2] Register                ║
║  [3] Exit                    ║
╚══════════════════════════════╝
```

---

## 🔑 Sample Credentials

These accounts are seeded automatically on the **first run**.

### Admin Account
| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Role | ADMIN — Full system access |

### Staff Account
| Field | Value |
|---|---|
| Username | `staff1` |
| Password | `staff123` |
| Role | STAFF — Reservation & payment processing |

### Customer / User Account
> Customers register themselves from the Main Menu using **[2] Register**.

**Registration requires:**
- A unique username (no spaces)
- A password (minimum 6 characters)
- A valid email address (e.g. `yourname@gmail.com`)
- A valid Philippine mobile number (format: `09XXXXXXXXX`)

**Sample values you can use during registration:**

| Field | Example |
|---|---|
| Username | `juan123` |
| Password | `pass123` |
| Email | `juan@gmail.com` |
| Mobile | `09171234567` |

> Passwords are stored encrypted using **Caesar Cipher** — they are never saved or displayed in plain text.

---

## 🔄 System Flow

```
MAIN MENU
│
├── [1] Login ──────────────────────────────────────────────┐
│     └── System detects role from credentials              │
│           ├── ADMIN  → Admin Control Panel                │
│           ├── STAFF  → Staff Control Panel                │
│           └── USER   → Customer Portal                    │
│                                                           │
├── [2] Register (Customer accounts only)                   │
│                                                           │
└── [3] Exit (saves all data to .txt files)                 │
                                                            │
RENTAL WORKFLOW:                                            │
                                                            │
  [USER] Reserve a Bike                                     │
      ↓ Status: PENDING                                     │
  [STAFF] View Pending Reservations                         │
      ↓                                                     │
  [STAFF] Confirm Reservation & Process Payment             │
      ↓ Status: CONFIRMED → Rental created (ACTIVE)         │
  [STAFF] Process Return                                    │
      ↓ Status: RETURNED → Stock restored                   │
```

---

## 👥 Role Capabilities

### 🔴 ADMIN
| Feature | Details |
|---|---|
| View All Motorbikes | See every bike including out-of-stock |
| View by Type / Brand | Filter by category or manufacturer |
| Search Bikes | By name, price range, or engine CC |
| Add Motorbike | Brand, model, type, color, CC, rate, stock |
| Remove Motorbike | Validates Bike ID format before removal |
| Update Bike Stock | Set stock to any number |
| View Out-of-Stock Bikes | Lists bikes with 0 stock |
| View All Rental Records | Full rental log with payment info |
| View All Reservations | All reservations regardless of status |
| View All Accounts | Lists all users, staff, and admins |
| Add Staff Account | Create a new staff account |

---

### 🟡 STAFF
| Feature | Details |
|---|---|
| View Available Motorbikes | Bikes with stock > 0 |
| View by Type / Brand | Filter and browse inventory |
| Search Bikes | By name, price range, or engine CC |
| View Pending Reservations | See customer requests awaiting approval |
| Confirm Reservation & Payment | Select payment method, generate receipt |
| Process Return | Mark active rental as returned, restore stock |
| View All Rental Records | Full history of all rental transactions |

---

### 🟢 USER / Customer
| Feature | Details |
|---|---|
| View Available Motorbikes | Bikes available to rent |
| View by Type / Brand | Browse by category or manufacturer |
| Search Bikes | By name, price range, or engine CC |
| Reserve a Motorbike | Choose bike + dates; saved as PENDING |
| View My Reservations | See status of your own reservations |
| View My Rental Status | See confirmed/active/returned rentals |
| My Favourites | Save, remove, and compare up to 5 bikes |

> **Important:** Customers cannot rent a bike directly. Reservations must be **confirmed by staff** with payment completed before a rental becomes active.

---

## 💳 Payment Methods

When staff processes a reservation, the following payment methods are available:

| Option | Method |
|---|---|
| [1] | GCash |
| [2] | PayMaya |
| [3] | PayPal |
| [4] | InstaPay |
| [5] | MariBank |
| [6] | Apple Pay |
| [7] | Cash |

After payment is selected and confirmed, the system automatically prints a **formatted receipt** showing all transaction details.

---

## 🗄️ Data Files

All data is saved to plain text files in the same directory as `MotoVault.java`. Files are **written on exit** and **read on startup**.

| File | Contents |
|---|---|
| `admin.txt` | Admin account credentials (Caesar-encrypted passwords) |
| `staff.txt` | Staff account credentials (Caesar-encrypted passwords) |
| `users.txt` | Customer accounts, passwords, and saved favourites |
| `data.txt` | All bikes, rental records, and reservation records |

> **Do not manually edit these files** unless you know the Caesar Cipher encoding (shift = 3). Corrupted files will be ignored on startup and default data will be reloaded.

---

## 📌 Notes

- **Maximum capacities:** 50 bikes · 30 users · 100 rentals · 100 reservations · 5 favourites per user
- **Date format:** Always enter dates as `DD/MM/YYYY` (e.g. `25/12/2025`)
- **Bike ID format:** System-generated as `MV-XXXX-01` (e.g. `MV-0001-01`)
- **Out-of-stock alert:** If confirming a reservation causes a bike's stock to reach 0, the system displays a warning automatically
- **Data persistence:** Always choose **[3] Exit** from the main menu (or **[0] Logout** then exit) to ensure data is saved properly. Closing the terminal window without exiting may lose unsaved changes

---

*MotoVault — Ride Managed. Ride Safe.* 🏍️
