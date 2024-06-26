/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.mavenproject1.models;

// imports from same package
import com.mycompany.mavenproject1.ModelObjects.Subscription;
import com.mycompany.mavenproject1.ModelObjects.Student;
import com.mycompany.mavenproject1.Exceptions.PhoneAlreadyExistsException;

// other imports
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.stream.Collectors;

/**
 *
 * @author AHMAD
 */
public final class StudentsModel extends Model {
    // TABLE ATTRIBUTES
    public static final String TABLE = "students";
    public static final String COL_ID = "student_id";
    public static final String COL_FULLNAME = "fullName";
    public static final String COL_PHONE = "phone";
    public static final String COL_GRADE = "grade";
    public static final String COL_LANGUAGE = "language";
    public static final String COL_SUBSCRIPTION_ID = "subscription_id";
    public static final String COL_MARK = "mark";
    
    private static StudentsModel model = null;
    public static StudentsModel getModel() throws SQLException, IOException {
        if(model == null) model = new StudentsModel();
        return model;
    }
    
    private final SubscriptionsModel subscriptionsModel;
    private StudentsModel() throws SQLException, IOException {
        this.subscriptionsModel = SubscriptionsModel.getModel();
        this.CreateTable();
    }
    
    @Override
    protected void CreateTable() throws SQLException, IOException {
        // Create tables
        String query = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" // TABLE
                    + "%s INTEGER PRIMARY KEY, " // ID
                    + "%s TEXT NOT NULL, " // FULL NAME
                    + "%s TEXT UNIQUE NOT NULL, " // PHONE
                    + "%s TEXT NOT NULL, " // GRADE
                    + "%s TEXT NOT NULL, " // LANGUAGE
                    + "%s INTEGER NOT NULL, " // SUBSCRIPTION_ID
                    + "%s FLOAT, " // MARK
                    + "FOREIGN KEY (%s) REFERENCES %s(%s)" // set foreign key (so that we can perform JOIN queries & create TRIGGERS easily)
                    + ")",
            TABLE,
            COL_ID,
            COL_FULLNAME,
            COL_PHONE,
            COL_GRADE,
            COL_LANGUAGE,
            COL_SUBSCRIPTION_ID,
            COL_MARK,
            COL_SUBSCRIPTION_ID, SubscriptionsModel.TABLE, SubscriptionsModel.COL_ID
        );
        
        // execute
        (new Query(query, null)).execute(database.connect());

        // create trigger (to delete subscription when student is deleted)
        this.createDeleteTrigger();
        
        // print out a message
        System.out.println(TABLE + " table created successfully.");
    }
    
    private void createDeleteTrigger() throws SQLException, IOException {
        String query = String.format(
                "CREATE TRIGGER IF NOT EXISTS delete_subscription "
                        + "AFTER DELETE ON %s " // this table
                        + "FOR EACH ROW "
                        + "BEGIN "
                        + "DELETE FROM %s WHERE %s = OLD.%s; "
                        + "END;",
                TABLE,
                SubscriptionsModel.TABLE,
                SubscriptionsModel.COL_ID,
                COL_SUBSCRIPTION_ID
        );
        (new Query(query, null)).execute(database.connect());
    }

    /*******************************************************************/
    // CRUD OPERATIONS
    
    // CREATE
    public void Create(Student s) throws PhoneAlreadyExistsException, SQLException, IOException {
        // check if student already exists
        if(this.existsStudent(s)) throw new PhoneAlreadyExistsException();

        // prepare query & its parameters
        String query = String.format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?);",
                TABLE, 
                COL_FULLNAME,
                COL_PHONE,
                COL_GRADE,
                COL_LANGUAGE,
                COL_SUBSCRIPTION_ID
        );
                
        List<Object> params = new ArrayList<>();
        params.add(s.getFullName());
        params.add(s.getPhone());
        params.add(s.getGrade());
        params.add(s.getLanguage());
//        params.add(s.getMark()); // we do not add mark here because the controller doesn't allow option of having mark
        
        // insert subscription
        subscriptionsModel.Create(s.getSubscription());
        // add its id to our query params
        params.add(subscriptionsModel.GetLastInsertedRowId()); // treat boolean as integer
        
        // execute query
        (new Query(query, params)).execute(database.connect());
    }
    
    /*******************************************************************/
    // READ
    
    public List<Student> Read() throws SQLException, IOException { return Read(null, null, null, null, null, null, null, null, null); }
    public List<Student> Read(Long id) throws SQLException, IOException { return Read(id, null, null, null, null, null, null, null, null); }
    public List<Student> Read(String fullName) throws SQLException, IOException { return Read(null, fullName, null, null, null, null, null, null, null); }
    public List<Student> Read(Subscription subscription) throws SQLException, IOException { return Read(null, null, null, null, null, subscription, null, null, null); }
    public List<Student> Read(
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Subscription subscription,
            Float minMark,
            Float maxMark,
            String marksOrder) throws SQLException, IOException {

        // prepare query
        Query query = this.queryFilter("SELECT * FROM " + TABLE, id, fullName, phone, grade, language, subscription, minMark, maxMark, marksOrder);
        
        // execute query
        ResultSet result = query.execute(database.connect());
        
        // fetch and return results
        List<Student> students = new ArrayList<>();
        while(result.next()) {
            // create student
            Student s = new Student();
            s.setId(result.getLong(COL_ID));
            s.setFullName(result.getString(COL_FULLNAME));
            s.setPhone(result.getString(COL_PHONE));
            s.setGrade(result.getString(COL_GRADE));
            s.setLanguage(result.getString(COL_LANGUAGE));
            s.setMark(result.getString(COL_MARK));
            s.setSubscription(subscriptionsModel.Read(result.getLong(COL_SUBSCRIPTION_ID)).get(0));
            
            // add student to results
            students.add(s);
        }
        
        // close resources
        result.close();
        
        // return
        return students;
    }
    
    /*******************************************************************/
    // UPDATE
    
    public void UpdateMark(long id, Float mark) throws SQLException, IOException {
        // set query
        String queryString = String.format("UPDATE %s SET %s = ? WHERE %s = ?", TABLE, COL_MARK, COL_ID);
        
        // set parameters
        List<Object> queryParams = new ArrayList<>();
        queryParams.add(mark);
        queryParams.add(id);
        
        // execute query
        (new Query(queryString, queryParams)).execute(database.connect());
    }
    
    public void Update(Student oldS, Student updatedS) throws SQLException, PhoneAlreadyExistsException, IOException {
        
        // check if updated phone number (UNIQUE attribute)
        if(oldS.getPhone().equals(updatedS.getPhone()) == false) {
            // check if the updated phone number already exists in the database
            if(this.existsStudent(updatedS)) throw new PhoneAlreadyExistsException();
        }
        
        /* update student information */
        // prepare query
        String query = String.format(
                "UPDATE %s SET %s=?, %s=?, %s=?, %s=? WHERE %s=?;",
                TABLE,
                COL_FULLNAME,
                COL_PHONE,
                COL_GRADE,
                COL_LANGUAGE,
                COL_ID
        );
        
        // set parameters
        List<Object> params = new ArrayList<>();
        params.add(updatedS.getFullName());
        params.add(updatedS.getPhone());
        params.add(updatedS.getGrade());
        params.add(updatedS.getLanguage());
        params.add(oldS.getId());
        
        // execute query
        (new Query(query, params)).execute(database.connect());

        // update subscription to match student's new subscription
        subscriptionsModel.Update(oldS.getSubscription(), updatedS.getSubscription());
    }
    
    /*******************************************************************/
    // DELETE    

    public void deleteStudentsByIDs(List<Long> IDs) throws SQLException, IOException {
        // Check if the list is empty
        if (IDs.isEmpty()) return;

        // Construct the IN clause with properly formatted phone numbers
        StringBuilder idListString = new StringBuilder();
        for (Long id : IDs) {
            if(id != null) idListString.append("?,");
        }

        // Remove the trailing comma
        idListString.deleteCharAt(idListString.length() - 1);

        // Construct the SQL query
        String query = String.format("DELETE FROM %s WHERE %s IN (%s)", TABLE, COL_ID, idListString.toString());

        // execute query
        List<Object> params = IDs.stream().map(Long::valueOf).collect(Collectors.toList());
        (new Query(query, params)).execute(database.connect());
    }
    
    public void Delete() throws SQLException, IOException { Delete(null, null, null, null, null, null, null); }
    public void Delete(
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Subscription subscription,
            Float mark) throws SQLException, IOException {
        
        // prepare the query
        Query query = this.queryFilter("DELETE FROM " + TABLE, id, fullName, phone, grade, language, subscription, mark);
        
        query.execute(database.connect());
    }
    
    /*******************************************************************/
    // PUBLIC GETTERS
    
    public boolean existsStudent(Student s) throws SQLException, IOException {
        // set query
        String query = String.format("SELECT * FROM %s WHERE %s = ?", TABLE, COL_PHONE);
        
        // set parameters
        List<Object> params = new ArrayList<>();
        params.add(s.getPhone());
        
        return (new Query(query, params)).execute(database.connect()).next();
    }
    
    // time complexity = O(1)
    public int getNumberOfStudents() throws SQLException, IOException {
        String query = "SELECT COUNT(*) AS rowCount FROM " + TABLE;
        return (new Query(query, null)).execute(database.connect()).getInt("rowCount");
    }
    
    public long getNumberOfActiveSubscriptions(
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Float minMark,
            Float maxMark,
            String marksOrder) throws SQLException, IOException {
        Query query = this.queryFilter("SELECT COUNT(*) AS activeSubscriptions FROM " + TABLE, id, fullName, phone, grade, language, new Subscription(null, true, null), maxMark);
        return query.execute(database.connect()).getLong("activeSubscriptions");
    }
    
    /*******************************************************************/
    
    // for Read()
    private Query queryFilter(
            String query,
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Subscription subscription,
            Float minMark,
            Float maxMark,
            String marksOrder) throws SQLException {
        return queryFilter(query, id, fullName, phone, grade, language, subscription, null, minMark, maxMark, marksOrder);
    }
    
    // for Delete()
    private Query queryFilter(
            String query,
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Subscription subscription,
            Float mark) throws SQLException {
        return queryFilter(query, id, fullName, phone, grade, language, subscription, mark, null, null, null);
    }
    
    private Query queryFilter(
            String query,
            Long id,
            String fullName,
            String phone,
            String grade,
            String language,
            Subscription subscription,
            Float mark,
            Float minMark,
            Float maxMark,
            String marksOrder) throws SQLException {
        
        // filter the query
        List<Object> params = new ArrayList<>();
        
        // id is unique
        if(id != null) {
            query += this.whereEqual(COL_ID);
            params.add(id);
            return new Query(query, params);
        }
        
        boolean first = true;
        if(subscription != null) {
            query += String.format(
                    " JOIN %s ON %s=%s", 
                    SubscriptionsModel.TABLE, 
                    SubscriptionsModel.TABLE + "." + SubscriptionsModel.COL_ID, 
                    TABLE + "." + COL_SUBSCRIPTION_ID);
            if(subscription.getId() != null) {
                query += this.whereEqual(SubscriptionsModel.COL_ID);
                params.add(subscription.getId());
                first = false;
            }
            if(subscription.getStatus() != null) {
                if(first) query += this.whereEqual(SubscriptionsModel.COL_STATUS);
                else query += this.andEqual(SubscriptionsModel.COL_STATUS);
                params.add(subscription.getStatus() ? Subscription.ACTIVE : Subscription.INACTIVE);
                first = false;
            }
            if(subscription.getDate() != null) {
                if(first) query += this.whereEqual(SubscriptionsModel.COL_EXPIRATION_DATE);
                else query += this.andEqual(SubscriptionsModel.COL_EXPIRATION_DATE);
                params.add(subscription.getDate().toString());
                first = false;
            }
            first = false;
        }
        if(fullName != null) {
            if(first) query += this.whereLike(COL_FULLNAME);
            else query += this.andLike(COL_FULLNAME);
            params.add("%" + fullName + "%");
            first = false;
        }
        if(phone != null) {
            if(first) query += this.whereLike(COL_PHONE);
            else query += this.andLike(COL_PHONE);
            params.add("%" + phone + "%");
            first = false;
        }
        if(grade != null) {
            if(first) query += this.whereLike(COL_GRADE);
            else query += this.andLike(COL_GRADE);
            params.add("%" + grade + "%");
            first = false;
        }
        if(language != null) {
            if(first) query += this.whereEqual(COL_LANGUAGE);
            else query += this.andEqual(COL_LANGUAGE);
            params.add(language);
            first = false;
        }
        if(mark != null) {
            if(first) query += this.whereEqual(COL_MARK);
            else query += this.andEqual(COL_MARK);
            params.add(mark);
            first = false;
        } else {
            if(minMark != null || maxMark != null) {
                if(first) {
                    if(minMark == null) {
                        // only use max
                        query += this.whereLessEqual(COL_MARK);
                        params.add(maxMark);
                    } else if(maxMark == null) {
                        // only use min
                        query += this.whereGreaterEqual(COL_MARK);
                        params.add(minMark);
                    } else {
                        // both were given
                        query += this.whereBetween(COL_MARK);
                        params.add(minMark);
                        params.add(maxMark);
                    }
                } else {
                    if(minMark == null) {
                        // only use max
                        query += this.andLessEqual(COL_MARK);
                        params.add(maxMark);
                    } else if(maxMark == null) {
                        // only use min
                        query += this.andGreaterEqual(COL_MARK);
                        params.add(minMark);
                    } else {
                        query += this.andBetween(COL_MARK);
                        params.add(minMark);
                        params.add(maxMark);
                    }
                }
            }
            // ORDER must be last
            if(marksOrder != null) {
                if(marksOrder.equalsIgnoreCase("asc")) query += this.orderByASC(COL_MARK);
                else query += this.orderByDESC(COL_MARK);
            }
        }
        
        return new Query(query, params);
    }
}
