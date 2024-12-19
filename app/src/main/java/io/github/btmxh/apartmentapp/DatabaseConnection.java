package io.github.btmxh.apartmentapp;

import java.io.*;
import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.commons.compiler.CompileException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private static final Logger logger = LogManager.getLogger(DatabaseConnection.class);

    public enum Role {
        ADMIN("Quản trị viên"),
        MANAGER( "Quản lý"),
        OFFICER( "Cảnh sát");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getSQLName() {
            return name();
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String getRoleEnum() {
            String roleEnum = Arrays.stream(Role.values())
                    .map(Role::getSQLName)
                    .map(role -> "'" + role + "'")
                    .collect(Collectors.joining(", "));
            return "Enum(" + roleEnum + ")";
        }

        public static Role getRole(String sqlName) {
            for (Role role : Role.values()) {
                if (role.name().equals(sqlName)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Giá trị không xác định: " + sqlName);
        }

        public static List<Role> getNonAdminRoles() {
            return Arrays.stream(Role.values()).filter(r -> r != Role.ADMIN).toList();
        }
    }

    public enum FeeType {
        MANAGEMENT("Phí quản lý"),
        SERVICE("Phí dịch vụ"),
        PARKING("Phí gửi xe"),
        DONATION("Phí đóng góp");

        private final String displayName;

        FeeType(String displayName) {
            this.displayName = displayName;
        }

        public String getSQLName() {
            return name();
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String getFeeTypeEnum() {
            String feeTypeEnum = Arrays.stream(FeeType.values())
                    .map(FeeType::getSQLName)
                    .map(type -> "'" + type + "'")
                    .collect(Collectors.joining(", "));
            return "Enum(" + feeTypeEnum + ")";
        }

        public static FeeType getFeeType(String sqlName) {
            for (FeeType type : FeeType.values()) {
                if (type.name().equals(sqlName)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Giá trị không xác định: " + sqlName);
        }
    }

    public enum Gender {
        MALE("Nam"),
        FEMALE("Nữ"),
        OTHER("Khác");

        private final String displayName;

        Gender(String displayName) {
            this.displayName = displayName;
        }

        public String getSQLName() {
            return name();
        }

        public String getDisplayName() {
            return displayName;
        }

        public static String getGenderEnum() {
            String genderEnum = Arrays.stream(Gender.values())
                    .map(Gender::getSQLName)
                    .map(type -> "'" + type + "'")
                    .collect(Collectors.joining(", "));
            return "Enum(" + genderEnum + ")";
        }

        public static Gender getGender(String sqlName) {
            for (Gender gender : Gender.values()) {
                if (gender.name().equals(sqlName)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("Giá trị không xác định: " + sqlName);
        }
    }

    private DatabaseConnection() {
        try {
            Dotenv dotenv = Dotenv.load();
            String url = dotenv.get("DB_URL");
            String username = dotenv.get("DB_USERNAME");
            String password = dotenv.get("DB_PASSWORD");
            if (url == null) {
                url = "jdbc:mysql://localhost:3306/apartment";
            }
            connection = DriverManager.getConnection(url, username, password);
            logger.info("Kết nối thành công tới Cơ sở dữ liệu");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối với Cơ sở dữ liệu", e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Không thể ngắt kết nối khỏi Cơ sở dữ liệu", e);
        }
    }

    public void createUsersTable() {
        String roleEnum = Role.getRoleEnum();
        String sql_createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                user_id INT PRIMARY KEY AUTO_INCREMENT,
                user_name VARCHAR(50) NOT NULL,
                user_fullname VARCHAR(50) NOT NULL,
                user_password VARCHAR(50) NOT NULL,
                user_phone_number VARCHAR(15) NOT NULL,
                """ +
                "user_role " + roleEnum + " NOT NULL);";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql_createUsersTable);
            logger.info("Đã tạo thành công bảng cho người dùng!");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng người dùng trên cơ sở dữ liệu!", e);
        }
    }

    public void createServiceFeeTable() {
        String typeEnum = FeeType.getFeeTypeEnum();
        String sql = """
                CREATE TABLE IF NOT EXISTS service_fees (
                fee_id INT PRIMARY KEY AUTO_INCREMENT,
                fee_name VARCHAR(50) NOT NULL,
                fee_value INT NOT NULL,
                fee_start_date DATE NOT NULL,
                fee_deadline DATE NOT NULL,
                value2 INT NOT NULL,
                """
                +
                "type " + typeEnum + " NOT NULL);";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Đã tạo thành công bảng phí dịch vụ!");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng phí dịch vụ trên cơ sở dữ liệu", e);
        }
    }

    public void createPaymentsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS payments (
                id INT PRIMARY KEY AUTO_INCREMENT,
                fee_id INT NOT NULL,
                room VARCHAR(10) NOT NULL,
                amount INT NOT NULL,
                commit_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                user_id INT NOT NULL);
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Đã tạo thành công bảng thanh toán!");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng thanh toán trên cơ sở dữ liệu", e);
        }
    }

    public void createCitizensTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS citizens (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    full_name VARCHAR(255) NOT NULL,
                    date_of_birth DATE NOT NULL,
                    gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL,
                    passport_id VARCHAR(12) NOT NULL UNIQUE,
                    nationality VARCHAR(100) NOT NULL,
                    room VARCHAR(10) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Đã tạo thành công bảng nhân khẩu!");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng nhân khẩu trên cơ sở dữ liệu", e);
        }
    }

    public void createRoomsTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS rooms (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(10) NOT NULL,
                    owner_id INT NOT NULL DEFAULT 0,
                    area FLOAT(1) NOT NULL,
                    number_of_motors INT NOT NULL DEFAULT 0,
                    number_of_cars INT NOT NULL DEFAULT 0
                );
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Đã tạo thành công bảng căn hộ!");
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng căn hộ trên cơ sở dữ liệu", e);
        }
    }

    public void addCitizenToDB(Citizen citizen) throws SQLException {
        String insertQuery = "INSERT INTO citizens (full_name, date_of_birth, gender, passport_id, nationality, room) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        String updateQuery = "UPDATE citizens SET full_name = ?, date_of_birth = ?, gender = ?, passport_id = ?, "
                + "nationality = ?, room = ? WHERE id = ?";

        // Nếu ID là -1 thì thực hiện thêm mới
        if (citizen.getId() == -1) {
            try (PreparedStatement st = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                st.setString(1, citizen.getFullName());
                st.setDate(2, Date.valueOf(citizen.getDateOfBirth()));
                st.setString(3, citizen.getGender().toString());
                st.setString(4, citizen.getPassportId());
                st.setString(5, citizen.getNationality());
                st.setString(6, citizen.getRoom());

                if (st.executeUpdate() == 0) {
                    throw new RuntimeException("Error while inserting citizen into DB");
                }

                // Lấy ID tự động tạo và gán lại cho đối tượng Citizen
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        citizen.setId(rs.getInt(1));
                    }
                }
            }
        }
        // Nếu ID khác -1 thì thực hiện cập nhật
        else {
            try (PreparedStatement st = connection.prepareStatement(updateQuery)) {
                st.setString(1, citizen.getFullName());
                st.setDate(2, Date.valueOf(citizen.getDateOfBirth()));
                st.setString(3, citizen.getGender().toString());
                st.setString(4, citizen.getPassportId());
                st.setString(5, citizen.getNationality());
                st.setString(6, citizen.getRoom());
                st.setInt(7, citizen.getId());

                if (st.executeUpdate() == 0) {
                    throw new RuntimeException("Error while updating citizen in DB");
                }
            }
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    private static JsonNode getJson(ResultSet rs, String key) throws IOException, SQLException {
        return mapper.readTree(rs.getBinaryStream(key));
    }

    public List<ServiceFee> getServiceFees(String query, int limit, int offset) throws IOException, SQLException {
        final var fees = new ArrayList<ServiceFee>();
        try(var st = connection.prepareStatement("SELECT fee_id, type, fee_name, fee_value, value2, fee_start_date, fee_deadline FROM service_fees WHERE INSTR(fee_name, ?) != 0 LIMIT ? OFFSET ?")) {
            st.setInt(2, limit);
            st.setInt(3, offset);
            st.setString(1, query);
            final var rs = st.executeQuery();
            while(rs.next()) {
                final var id = rs.getInt("fee_id");
                final var type = FeeType.valueOf(rs.getString("type"));
                final var name = rs.getString("fee_name");
                final var value1 = rs.getLong("fee_value");
                final var value2 = rs.getLong("value2");
                final var startDate = rs.getDate("fee_start_date").toLocalDate();
                final var deadline = rs.getDate("fee_deadline").toLocalDate();
//                int received, pending;
//                try(var st2 = connection.prepareStatement("SELECT COUNT(*) FROM payments WHERE fee_id = ?")) {
//                    st2.setInt(1, id);
//                    var r2 = st2.executeQuery();
//                    r2.next();
//                    received = r2.getInt(1);
//                }
//                try(var st2 = connection.prepareStatement("SELECT COUNT(*) FROM citizens WHERE created_at <= ?")) {
//                    st2.setDate(1, Date.valueOf(startDate));
//                    var r2 = st2.executeQuery();
//                    r2.next();
//                    pending = r2.getInt(1);
//                }

                final var fee = new ServiceFee(id, type, name, value1, value2, startDate, deadline);
                fees.add(fee);
            }
        }
        return fees;
    }

    public List<Room> getRooms(String query) throws SQLException {
        final var rooms = new ArrayList<Room>();
        try(var st = connection.prepareStatement("SELECT id, name, owner_id, area, number_of_motors, number_of_cars FROM rooms WHERE INSTR(name, ?) != 0")) {
            st.setString(1, query);
            final var rs = st.executeQuery();
            while(rs.next()) {
                final var id = rs.getInt("id");
                final var name = rs.getString("name");
                final var ownerId = rs.getInt("owner_id");
                final var area = rs.getFloat("area");
                final var numMotors = rs.getInt("number_of_motors");
                final var numCars = rs.getInt("number_of_cars");
                final var room = new Room(id, name, ownerId, area, numMotors, numCars);
                rooms.add(room);
            }
        }
        return rooms;
    }

    public List<ServiceFee> getUnchargedFees(String query, Room room) throws IOException, SQLException {
        final var fees = new ArrayList<ServiceFee>();
        try(var st = connection.prepareStatement("SELECT fee_id, fee_name, fee_value, type, value2 FROM service_fees WHERE INSTR(fee_name, ?) != 0 AND fee_id NOT IN (SELECT fee_id FROM payments WHERE room = ?)")) {
            st.setString(1, query);
            st.setString(2, room.getName());
            final var rs = st.executeQuery();
            while(rs.next()) {
                final var id = rs.getInt("fee_id");
                final var type = FeeType.valueOf(rs.getString("type"));
                final var name = rs.getString("fee_name");
                final var value1 = rs.getLong("fee_value");
                final var value2 = rs.getLong("value2");
                final var fee = new ServiceFee(id, type, name, value1, value2);
                fees.add(fee);
            }
        }
        return fees;
    }

    public boolean paymentExists(int fee_id, String room) throws SQLException, IOException {
        try(var st = connection.prepareStatement("SELECT * FROM payments WHERE fee_id = ? AND room = ?")) {
            st.setInt(1, fee_id);
            st.setString(2, room);
            final var rs = st.executeQuery();
            return rs.next();
        }
    }

    public void updatePayment(Payment p) throws SQLException, IOException {
        if(p.getId() == Payment.NULL_ID) {
            try(var st = connection.prepareStatement("INSERT INTO payments (fee_id, room, amount) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                st.setInt(1, p.getFee().getId());
                st.setString(2, p.getRoomId());
                st.setLong(3, p.getAmount());

                if(st.executeUpdate() == 0) {
                    throw new RuntimeException("Lỗi khi chèn phí dịch vụ vào DB");
                }
                try(var rs = st.getGeneratedKeys()) {
                    rs.next();
                    p.setId(rs.getInt(1));
                }
            }
        } else {
            try(var st = connection.prepareStatement("UPDATE payments SET fee_id = ?, room = ?, amount = ? WHERE id = ?")) {
                st.setInt(1, p.getFee().getId());
                st.setString(2, p.getRoomId());
                st.setLong(3, p.getAmount());
                st.setInt(4, p.getId());
                st.executeUpdate();
            }
        }
    }

    public void updatePayment1(Payment p) throws SQLException, IOException {
        try(var st = connection.prepareStatement("INSERT INTO payments (fee_id, room, amount, user_id) VALUES (?, ?, ?, ?)")) {
            st.setInt(1, p.getFee().getId());
            st.setString(2, p.getRoomId());
            st.setLong(3, p.getAmount());
            st.setInt(4, p.getUser().getId());
            st.executeUpdate();
        }
    }

    public String getRoomOwner(String room) throws SQLException, IOException {
        try(var st = connection.prepareStatement("SELECT full_name FROM citizens WHERE room = ?")) {
            st.setString(1, room);
            var rs = st.executeQuery();
            if(rs.next()) {
                return rs.getString("full_name");
            } else {
                return null;
            }
        }
    }

    public int getRoomArea(String room) throws SQLException, IOException {
        try(var st = connection.prepareStatement("SELECT area FROM rooms WHERE room = ?")) {
            st.setString(1, room);
            var rs = st.executeQuery();
            if(rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }

//        String sql = "SELECT user_role FROM users WHERE user_name = ?;";
//        try (PreparedStatement ps = connection.prepareStatement(sql)) {
//            ps.setString(1, username);
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    return rs.getString("user_role");
//                } else {
//                    return "";
//                }
//            }
//        }
    }

    public void updateServiceFee(ServiceFee fee, long oldAmount) throws SQLException, IOException {
        if(fee.getId() == ServiceFee.NULL_ID) {
            try(var st = connection.prepareStatement("INSERT INTO service_fees (fee_name, fee_value, fee_start_date, fee_deadline) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                st.setString(1, fee.getName());
                st.setLong(2, fee.getAmount());
                st.setDate(3, Date.valueOf(fee.getStartDate()));
                st.setDate(4, Date.valueOf(fee.getDeadline()));

                if(st.executeUpdate() == 0) {
                    throw new RuntimeException("Lỗi khi chèn phí dịch vụ vào DB");
                }
                try(var rs = st.getGeneratedKeys()) {
                    rs.next();
                    fee.setId(rs.getInt(1));
                }
            }
        } else {
            if(oldAmount != fee.getAmount()) {
                try(var st = connection.prepareStatement("UPDATE payments SET amount = ? WHERE fee_id = ?")) {
                    st.setLong(1, oldAmount);
                    st.setInt(2, fee.getId());
                    st.executeUpdate();
                }
            }
            try(var st = connection.prepareStatement("UPDATE service_fees SET fee_name = ?, fee_value = ?, fee_start_date = ?, fee_deadline = ? WHERE fee_id = ?")) {
                st.setString(1, fee.getName());
                st.setLong(2, fee.getAmount());
                st.setDate(3, Date.valueOf(fee.getStartDate()));
                st.setDate(4, Date.valueOf(fee.getDeadline()));
                st.setInt(5, fee.getId());
                st.executeUpdate();
            }
        }
    }

    public void updateServiceFee1(ServiceFee fee) throws SQLException, IOException {
        if(fee.getId() == ServiceFee.NULL_ID) {
            try (var st = connection.prepareStatement("INSERT INTO service_fees (type, fee_name, fee_value, value2, fee_start_date, fee_deadline) VALUES (?, ?, ?, ?, ?, ?)")) {
                st.setString(1, fee.getType().getSQLName());
                st.setString(2, fee.getName());
                st.setLong(3, fee.getValue1());
                st.setLong(4, fee.getValue2());
                st.setDate(5, Date.valueOf(fee.getStartDate()));
                st.setDate(6, Date.valueOf(fee.getDeadline()));
                st.executeUpdate();
            }
        }
        else {
            try (var st = connection.prepareStatement("UPDATE service_fees SET type = ?, fee_name = ?, fee_value = ?, value2 = ?, fee_start_date = ?, fee_deadline = ? WHERE fee_id = ?")) {
                st.setString(1, fee.getType().getSQLName());
                st.setString(2, fee.getName());
                st.setLong(3, fee.getValue1());
                st.setLong(4, fee.getValue2());
                st.setDate(5, Date.valueOf(fee.getStartDate()));
                st.setDate(6, Date.valueOf(fee.getDeadline()));
                st.setInt(7, fee.getId());
                st.executeUpdate();
            }
        }
    }

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_name = ? AND user_password = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if(!rs.next()) {
                    return null;
                }
                return new User(rs.getInt("user_id"), rs.getString("user_name"), rs.getString("user_fullname"), rs.getString("user_phone_number"), Role.valueOf(rs.getString("user_role")));
            }
        }
    }

    public boolean signup(String username, String name, String phoneNumber, String password) throws SQLException {
        String sql = "SELECT user_name FROM users WHERE user_name = ?;";
        try (PreparedStatement ps1 = connection.prepareStatement(sql)) {
            ps1.setString(1, username);
            try (ResultSet rs = ps1.executeQuery()) {
                if (rs.next()) {
                    return false;
                } else {
                    String role = isFirstAccount() ? Role.ADMIN.getSQLName() : Role.MANAGER.getSQLName();
                    String sql2 = "INSERT INTO users (user_name, user_fullname, user_phone_number, user_password, user_role) VALUES (?, ?, ?, ?, ?);";
                    try (PreparedStatement ps2 = connection.prepareStatement(sql2)) {
                        ps2.setString(1, username);
                        ps2.setString(2, name);
                        ps2.setString(3, phoneNumber);
                        ps2.setString(4, password);
                        ps2.setString(5, role);
                        ps2.executeUpdate();
                    }
                    return true;
                }
            }
        }
    }

    public long calc(String sql) throws SQLException {
        try(Statement st = connection.createStatement()) {
            final var rs = st.executeQuery(sql);
            rs.next();
            return rs.getLong(1);
        }
    }

    public long[] getDashboardValues() throws SQLException {
        return new long[]{
            calc("SELECT SUM(IF(fee_value > amount, fee_value, amount)) FROM payments INNER JOIN service_fees on payments.fee_id = service_fees.fee_id"),
            calc("SELECT SUM(IF(f.fee_value > 0, f.fee_value, 0) * (SELECT COUNT(*) FROM citizens c WHERE f.fee_start_date >= c.created_at)) FROM service_fees f"),
            calc("SELECT COUNT(1) FROM citizens"),
            calc("SELECT COUNT(DISTINCT room) FROM citizens"),
        };
    }

    public ObservableList<XYChart.Series<String, Integer>> getChartData() throws SQLException {
        String sql = """
                SELECT
                    YEAR(f.fee_deadline) AS year,
                    MONTH(f.fee_deadline) AS month,
                    SUM(f.fee_value * (SELECT COUNT(*) FROM citizens c WHERE f.fee_start_date >= c.created_at )) AS pending
                FROM
                    service_fees f
                WHERE
                    f.fee_value > 0
                GROUP BY
                    YEAR(f.fee_deadline),
                    MONTH(f.fee_deadline)
                ORDER BY
                    year, month
                LIMIT 10;
                """;


        var list = FXCollections.<XYChart.Series<String, Integer>>observableArrayList();
        try(final var st = connection.createStatement()) {
            final var rs = st.executeQuery(sql);
            final ObservableList<XYChart.Data<String, Integer>> data = FXCollections.observableArrayList();
            while(rs.next()) {
                var ym = YearMonth.of(rs.getInt("year"), rs.getInt("month")).toString();
                int pending = (int) rs.getLong("pending");
                data.add(new XYChart.Data<>(ym, pending));
            }
            final var series = new XYChart.Series<String, Integer>();
            series.setData(data);
            series.setName("Tổng số tiền cần thu");
            list.add(series);
        }

        sql = """
                SELECT
                    YEAR(f.fee_deadline) AS year,
                    MONTH(f.fee_deadline) AS month,
                    SUM(IF(f.fee_value > p.amount, f.fee_value, p.amount)) AS received
                FROM
                    payments p
                INNER JOIN
                    service_fees f ON p.fee_id = f.fee_id
                GROUP BY
                    YEAR(f.fee_deadline),
                    MONTH(f.fee_deadline)
                ORDER BY
                    year, month
                LIMIT 10;
                """;
        try(final var st = connection.createStatement()) {
            final var rs = st.executeQuery(sql);
            final ObservableList<XYChart.Data<String, Integer>> data = FXCollections.observableArrayList();
            while(rs.next()) {
                var ym = YearMonth.of(rs.getInt("year"), rs.getInt("month")).toString();
                int received = (int) rs.getLong("received");
                data.add(new XYChart.Data<>(ym, received));
            }
            final var series = new XYChart.Series<String, Integer>();
            series.setData(data);
            series.setName("Tổng số tiền đã thu");
            list.add(series);
        }

        return list;
    }

    public String getRole(String username) throws SQLException {
        String sql = "SELECT user_role FROM users WHERE user_name = ?;";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_role");
                } else {
                    return "";
                }
            }
        }
    }

    public boolean isFirstAccount() throws SQLException {
        String sql = "SELECT * FROM users;";
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery(sql)) {

            return !rs.next();
        }
    }

    public void resetDatabase() throws SQLException {
        try (final var st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS users");
            st.execute("DROP TABLE IF EXISTS service_fees");
            st.execute("DROP TABLE IF EXISTS payments");
            st.execute("DROP TABLE IF EXISTS citizens");
            logger.info("Successfully reset database");
        }
    }

    public ObservableList<User> getNonAdminUserList(int limit, int offset) throws SQLException {
        ObservableList<User> userList = FXCollections.observableArrayList();
        String query = "SELECT user_id, user_name, user_fullname, user_phone_number, user_role FROM users WHERE user_role != '" + Role.ADMIN.getSQLName() + "' LIMIT " + limit + " OFFSET " + offset;
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("user_id");
                String name = rs.getString("user_name");
                String fullname = rs.getString("user_fullname");
                String phoneNum = rs.getString("user_phone_number");
                String role = rs.getString("user_role");
                userList.add(new User(id, name, fullname, phoneNum, Role.getRole(role)));
            }
        }
        return userList;
    }

    public void setRole(String username, Role role) throws SQLException {
        String query = "UPDATE users SET user_role = ? WHERE user_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, role.getSQLName());
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    public int getNumNonAdminUsers() throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE user_role != '" + Role.ADMIN.getSQLName() + "'";
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public int getNumServiceFees(String search) throws SQLException {
        String query = "SELECT COUNT(*) FROM service_fees WHERE INSTR(fee_name, ?) != 0";
        try (PreparedStatement s = connection.prepareStatement(query)) {
            s.setString(1, search);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public int getNumPayments() throws SQLException {
        String query = "SELECT COUNT(*) FROM payments";
        try (PreparedStatement s = connection.prepareStatement(query)) {
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public int getNumPayment() throws SQLException {
        String query = "SELECT COUNT(*) FROM payments";
        try (PreparedStatement s = connection.prepareStatement(query)) {
//            s.setString(1, search);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public int getNumResident() throws SQLException {
        String query = "SELECT COUNT(*) FROM citizens";
        try (PreparedStatement s = connection.prepareStatement(query)) {
//            s.setString(1, search);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public void removeUser(int id) throws SQLException {
        try(var st = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }

    public void removeServiceFee(ServiceFee fee) throws SQLException {
        try(var st = connection.prepareStatement("DELETE FROM service_fees WHERE fee_id = ?")) {
            st.setInt(1, fee.getId());
            st.executeUpdate();
        }
    }

    public  void removeResident(int id) throws SQLException {
        try(var st = connection.prepareStatement("DELETE FROM citizens WHERE id = ?")){
            st.setInt(1,id);
            st.executeUpdate();
        }
    }

    public List<Payment> getPayments(String feeQ, String roomQ, int limit, int offset) throws SQLException {
        var list = new ArrayList<Payment>();
        try(var st = connection.prepareStatement("""
                SELECT
                    payments.*,
                    service_fees.*,
                    citizens.full_name AS owner_name
                FROM
                    payments
                JOIN
                    citizens
                ON
                    payments.room = citizens.room
                JOIN
                    service_fees
                ON
                    payments.fee_id = service_fees.fee_id
                WHERE
                    INSTR(payments.room, ?) != 0
                    AND INSTR(service_fees.fee_name, ?) != 0
                LIMIT ? OFFSET ?;
                """)) {
            st.setString(1, roomQ);
            st.setString(2, feeQ);
            st.setInt(3, limit);
            st.setInt(4, offset);
            final var rs = st.executeQuery();
            while(rs.next()) {
                list.add(new Payment(
                        rs.getInt("payments.id"),
                        new ServiceFee(
                                rs.getInt("service_fees.fee_id"),
                                rs.getString("service_fees.fee_name"),
                                rs.getLong("service_fees.fee_value"),
                                rs.getDate("service_fees.fee_start_date").toLocalDate(),
                                rs.getDate("service_fees.fee_deadline").toLocalDate()
                        ),
                        rs.getString("payments.room"),
                        rs.getLong("payments.amount"),
                        rs.getTimestamp("payments.commit_timestamp").toLocalDateTime(),
                        rs.getString("owner_name")
                ));
            }
        }

        return list;
    }

    public ObservableList<Citizen> getResidents(String search, int limit, int offset) throws SQLException {
        ObservableList<Citizen> residentList = FXCollections.observableArrayList();
        String query = """
                SELECT * FROM Citizens WHERE INSTR(full_name, ?) > 0 LIMIT ? OFFSET ?""";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, search);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String fullname = rs.getString("full_name");
                    Date date = rs.getDate("date_of_birth");
                    String gender = rs.getString("gender");
                    String passportId = rs.getString("passport_id");
                    String nationality = rs.getString("nationality");
                    String room = rs.getString("room");
                    var createdAt = rs.getTimestamp("created_at");
                    var updatedAt = rs.getTimestamp("updated_at");

                    residentList.add(new Citizen(id, fullname, date.toLocalDate(), Gender.valueOf(gender), passportId, nationality, room, createdAt.toLocalDateTime(), updatedAt.toLocalDateTime()));
                }
            }
        }
        return residentList;
    }
}
