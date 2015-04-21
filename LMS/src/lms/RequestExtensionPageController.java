/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lms;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author zain
 */
public class RequestExtensionPageController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Button submitButton;
    @FXML
    private TextField issueID;
    @FXML
    private TextField currentExtensionDate;
    @FXML
    private TextField currentReturnDate;
    @FXML
    private TextField originalCheckoutDate;
    @FXML
    private TextField newExtensionDate;
    @FXML
    private TextField newReturnDate;
    @FXML
    private Button submitExtensionRequest;
    @FXML
    private Button backButton;
    @FXML
    private Text  error;
    @FXML
    private Text success;
    private boolean enteredIssueID;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    @FXML
    public void submitExtensionRequestPressed(MouseEvent event) {
        success.setText("");
        String id = issueID.getText();
        int isStudent = checkStudentOrStaff(); //1 if student, 0 if staff
        Connection con = null;
        int countExtensions = countNumberOfExtensions();
        if (!enteredIssueID) {
            error.setText("First enter valid Issue ID");
        } else {
            if (checkForFutureRequester(id)) {
                error.setText("Extension denied: there is a future requester");
            } else if (isStudent == 1 && countExtensions >= 2) {
                error.setText("Out of Extensions");
            } else if (isStudent == 0 && countExtensions >= 4) {
                error.setText("Out of Extensions");
            } else {
                try {
                    error.setText("");
                    Class.forName("com.mysql.jdbc.Driver").newInstance();
                    con = DriverManager.getConnection("jdbc:mysql://academic-mysql.cc.gatech.edu/cs4400_Group_22", "cs4400_Group_22",
                    "ayt2V3Ck");

                    Statement stmt = con.createStatement();
                    stmt.executeUpdate("UPDATE ISSUES\n" + 
                            "SET Extension_date = CURDATE(), Return_date = DATE_ADD(CURDATE(), INTERVAL 14 DAY), \n" +
                            "Count_of_extensions = " + (countExtensions + 1) + "\n" +
                            "WHERE Issue_ID = '" + id + "'");
                    success.setText("Done!");
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                } finally {
                    try {
                        if (con != null) {
                            con.close();
                        }
                    } catch (SQLException e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
    }
    @FXML
    public void submitButtonPressed(MouseEvent event) {
        String id = issueID.getText();
        Connection con = null;
        success.setText("");
        if (id.equals("")) {
            error.setText("Issue ID can't be empty");
        } else {
            try {
                error.setText("");
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                con = DriverManager.getConnection("jdbc:mysql://academic-mysql.cc.gatech.edu/cs4400_Group_22", "cs4400_Group_22",
                "ayt2V3Ck");

                Statement stmt = con.createStatement();
                ResultSet results =  stmt.executeQuery("SELECT Date_of_issue, Return_date, Extension_date\n" + 
                        "FROM ISSUES\n" + 
                        "WHERE ISSUES.Issue_id = " + id + "\n");
                String originalCheckout = "";
                while (results.next()) {
                    originalCheckout = results.getString("Date_of_issue");
                    currentExtensionDate.setText(results.getString("Extension_date"));
                    currentReturnDate.setText(results.getString("Return_date"));
                }
                if (originalCheckout.equals("")) {
                    error.setText("Invalid Issue ID");
                } else {
                    originalCheckoutDate.setText(originalCheckout);
                    ResultSet moreResults = stmt.executeQuery("SELECT CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY)");
                    while(moreResults.next()) {
                        newReturnDate.setText(moreResults.getString("DATE_ADD(CURDATE(), INTERVAL 14 DAY)"));
                        newExtensionDate.setText(moreResults.getString("CURDATE()"));
                    }
                    enteredIssueID = true;
                }
                
            } catch (Exception e) {
                System.err.println(e.getMessage());
            } finally {
                try {
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
    public void backButtonPressed() {
        try {
            Parent foster = LMS.getParent();
            Stage stage = LMS.getStage();
            foster = FXMLLoader.load(getClass().getResource("Home.fxml"));

            Scene scene = new Scene(foster);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private Boolean checkForFutureRequester(String issueId) {
        Connection con = null;

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://academic-mysql.cc.gatech.edu/cs4400_Group_22", "cs4400_Group_22",
            "ayt2V3Ck");

            Statement stmt = con.createStatement();
            ResultSet results =  stmt.executeQuery("SELECT COUNT(*)\n" +
                                                   "FROM ISSUES\n" +
                                                   "INNER JOIN BOOK_COPY\n" +
                                                   "ON ISSUES.I_isbn = BOOK_COPY.C_isbn AND ISSUES.I_copy_no = BOOK_COPY.Copy_number\n" +
                                                   "WHERE ISSUES.Issue_id = " + issueId + " AND Future_requester IS NOT NULL");
            while (results.next()) {
                return Integer.parseInt(results.getString("COUNT(*)")) == 1;
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
        return false;
    }

    private int checkStudentOrStaff() {
        String id = issueID.getText();
        Connection con = null;
        int isStudent = 1;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://academic-mysql.cc.gatech.edu/cs4400_Group_22", "cs4400_Group_22",
            "ayt2V3Ck");

            Statement stmt = con.createStatement();
            ResultSet results =  stmt.executeQuery("SELECT COUNT(*) FROM (\n" +
                    "SELECT * \n" +
                    "FROM ISSUES\n" +
                    "INNER JOIN STUDENT_FACULTY\n" +
                    "ON STUDENT_FACULTY.Sf_username = ISSUES.I_sf_username\n" +
                    "WHERE ISSUES.Issue_id = '" + id + "'\n" +
                    ") T1");
            while (results.next()) {
                isStudent = Integer.parseInt(results.getString("COUNT(*)"));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
        return isStudent;
    }

    private int countNumberOfExtensions() {
        Connection con = null;
        int extensions = 0;
        String id = issueID.getText();
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://academic-mysql.cc.gatech.edu/cs4400_Group_22", "cs4400_Group_22",
            "ayt2V3Ck");

            Statement stmt = con.createStatement();
            ResultSet results =  stmt.executeQuery("SELECT Count_of_Extensions\n" +
                    "FROM ISSUES\n" +
                    "WHERE ISSUES.Issue_id = '" + id + "'");
            while (results.next()) {
                extensions = Integer.parseInt(results.getString("Count_of_Extensions"));
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        }
        return extensions;
    }
}
