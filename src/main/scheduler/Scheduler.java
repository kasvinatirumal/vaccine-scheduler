package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

import java.util.regex.Pattern;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most 2one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if the password is strong
        boolean isStrong = Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[!@#?]).{8,}$")
                .matcher(password)
                .find();

        if(!isStrong) {
            System.out.println("Password must contain at least 8 characters, an uppercase letter, a lowercase letter, " +
                    "a number, and a special character from (!, @, #, ?)");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }

        // check 3: check if the password is strong
        boolean isStrong = Pattern.compile("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[!@#?]).{8,}$")
                .matcher(password)
                .find();

        if(!isStrong) {
            System.out.println("Password must contain at least 8 characters, an uppercase letter, a lowercase letter, " +
                    "a number, and a special character from (!, @, #, ?)");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in first.");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Search failed.");
            return;
        }

        String date = tokens[1];
        Date d = null;

        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again! Enter a valid date in the format YYYY-MM-DD!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectCaregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String selectVaccines = "SELECT Name, Doses FROM Vaccines ORDER BY Name";

        try {
            PreparedStatement care_statement = con.prepareStatement(selectCaregivers);
            care_statement.setDate(1, d);
            ResultSet caregiverList = care_statement.executeQuery();

            while (caregiverList.next()) {
                System.out.println(caregiverList.getString("Username"));
            }

            PreparedStatement vac_statement = con.prepareStatement(selectVaccines);
            ResultSet vaccineList = vac_statement.executeQuery();

            while (vaccineList.next()) {
                String vaccine = vaccineList.getString("Name");
                int doses = vaccineList.getInt("Doses");
                System.out.println(vaccine + " " + doses);
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when checking availabilities");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if(currentCaregiver != null) {
            System.out.println("Please login as a patient.");
            return;
        }

        if (currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please try again.");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        Date d = null;
        try {
            d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again! Enter a valid date in the format YYYY-MM-DD!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkCaregivers = "SELECT TOP 1 Username FROM Availabilities WHERE Time = ? ORDER BY Username;";
        String checkVaccines = "SELECT Doses FROM Vaccines WHERE Name = ?";
        String addAppointment = "INSERT INTO Appointments (Patient, Caregiver, Vaccine, Time) VALUES (?, ?, ?, ?)";
        String deleteAvailability = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";

        try {
            PreparedStatement getCaregiverStatement = con.prepareStatement(checkCaregivers);
            getCaregiverStatement.setDate(1, d);
            ResultSet caregiverList = getCaregiverStatement.executeQuery();

            PreparedStatement checkVaccinesStatement = con.prepareStatement(checkVaccines);
            checkVaccinesStatement.setString(1, vaccine);
            ResultSet vaccineList = checkVaccinesStatement.executeQuery();

            String caregiver = "";
            while (caregiverList.next()) {
                caregiver = caregiverList.getString("Username");
            }

            int doses = 0;
            while (vaccineList.next()) {
                doses = vaccineList.getInt("Doses");
            }

            if (caregiver.isEmpty()) {
                System.out.println("No caregiver is available!");
            }
            if (doses == 0) {
                System.out.println("Not enough available doses!");
            }
            if (caregiver.isEmpty() || doses == 0) {
                return;
            }

            Vaccine currVaccine = new Vaccine.VaccineBuilder(vaccine, doses).build();

            // Insert new appointment into table
            PreparedStatement addAppointmentStatement = con.prepareStatement(addAppointment, Statement.RETURN_GENERATED_KEYS);
            addAppointmentStatement.setString(1, currentPatient.getUsername());
            addAppointmentStatement.setString(2, caregiver);
            addAppointmentStatement.setString(3, currVaccine.getVaccineName());
            addAppointmentStatement.setDate(4, d);

            // Print appointment to output
            addAppointmentStatement.executeUpdate();
            ResultSet addedAppointment = addAppointmentStatement.getGeneratedKeys();
            if (addedAppointment.next()) {
                int newId = addedAppointment.getInt(1);
                System.out.println("Appointment ID " + newId + ", Caregiver username " + caregiver);
            }

            // Update caregiver availability
            PreparedStatement deleteAvailabilityStmt = con.prepareStatement(deleteAvailability);
            deleteAvailabilityStmt.setDate(1, d);
            deleteAvailabilityStmt.setString(2, caregiver);
            deleteAvailabilityStmt.executeUpdate();

            // Update vaccine availability
            currVaccine.decreaseAvailableDoses(1);

        } catch (SQLException e) {
            System.out.println("Please try again.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String checkCaregiverRsv = "SELECT ID, Vaccine, Time, Patient FROM Appointments WHERE Caregiver = ?";
        String checkPatientRsv = "SELECT ID, Vaccine, Time, Caregiver FROM Appointments WHERE Patient = ?";

        try {
            if (currentCaregiver != null) {
                PreparedStatement caregiverRsvStatement = con.prepareStatement(checkCaregiverRsv);
                caregiverRsvStatement.setString(1, currentCaregiver.getUsername());
                ResultSet caregiverRsvList = caregiverRsvStatement.executeQuery();

                if (!caregiverRsvList.isBeforeFirst()) {
                    System.out.println("No appointments scheduled!");
                } else {
                    while (caregiverRsvList.next()) {
                        int ID = caregiverRsvList.getInt("ID");
                        String vaccine = caregiverRsvList.getString("Vaccine");
                        Date time = caregiverRsvList.getDate("Time");
                        String patient = caregiverRsvList.getString("Patient");

                        System.out.println(ID + " " + vaccine + " " + time + " " + patient);
                    }
                }
            } else {
                PreparedStatement patientRsvStatement = con.prepareStatement(checkPatientRsv);
                patientRsvStatement.setString(1, currentPatient.getUsername());
                ResultSet patientRsvList = patientRsvStatement.executeQuery();

                if (!patientRsvList.isBeforeFirst()) {
                    System.out.println("No appointments scheduled!");
                } else {
                    while (patientRsvList.next()) {
                        int ID = patientRsvList.getInt("ID");
                        String vaccine = patientRsvList.getString("Vaccine");
                        Date time = patientRsvList.getDate("Time");
                        String caregiver = patientRsvList.getString("Caregiver");

                        System.out.println(ID + " " + vaccine + " " + time + " " + caregiver);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // Asks the user to try again if they enter additional characters after logout
        if (tokens.length > 1) {
            System.out.println("Please try again.");
            return;
        }

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // Logout
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out.");
    }
}