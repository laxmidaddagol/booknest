import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookNest extends Application {
    private TableView<Book> table = new TableView<>();
    private TextField titleField = new TextField();
    private TextField authorField = new TextField();
    private TextField genreField = new TextField();
    private ChoiceBox<String> statusBox = new ChoiceBox<>();
    private TextField searchField = new TextField();
    private ObservableList<Book> books = FXCollections.observableArrayList();

    public static void main(String[] args) {
        initDB();
        launch(args);
    }

    public void start(Stage stage) {
        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(c -> c.getValue().titleProperty());
        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(c -> c.getValue().authorProperty());
        TableColumn<Book, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(c -> c.getValue().genreProperty());
        TableColumn<Book, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> c.getValue().statusProperty());
        table.getColumns().addAll(titleCol, authorCol, genreCol, statusCol);

        statusBox.getItems().addAll("Reading", "Finished");

        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> addBook());
        Button delBtn = new Button("Delete");
        delBtn.setOnAction(e -> deleteBook());
        Button markBtn = new Button("Mark Finished");
        markBtn.setOnAction(e -> markFinished());
        Button searchBtn = new Button("Search");
        searchBtn.setOnAction(e -> search());

        HBox addBox = new HBox(5, titleField, authorField, genreField, statusBox, addBtn);
        HBox.setHgrow(titleField, Priority.ALWAYS);
        HBox searchBox = new HBox(5, searchField, searchBtn, delBtn, markBtn);
        VBox topBox = new VBox(8, addBox, searchBox);
        topBox.setPadding(new Insets(10));

        loadData();
        VBox root = new VBox(10, topBox, table);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 650, 500));
        stage.setTitle("BookNest");
        stage.show();
    }

    private void addBook() {
        if (titleField.getText().isEmpty() || statusBox.getValue() == null) return;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO books (title,author,genre,status) VALUES (?,?,?,?)")) {
            ps.setString(1, titleField.getText());
            ps.setString(2, authorField.getText());
            ps.setString(3, genreField.getText());
            ps.setString(4, statusBox.getValue());
            ps.executeUpdate();
        } catch (SQLException e) {}
        clearFields();
        loadData();
    }

    private void deleteBook() {
        Book b = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id=?")) {
            ps.setInt(1, b.id);
            ps.executeUpdate();
        } catch (SQLException e) {}
        loadData();
    }

    private void markFinished() {
        Book b = table.getSelectionModel().getSelectedItem();
        if (b == null) return;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("UPDATE books SET status='Finished' WHERE id=?")) {
            ps.setInt(1, b.id);
            ps.executeUpdate();
        } catch (SQLException e) {}
        loadData();
    }

    private void search() {
        String q = searchField.getText().toLowerCase();
        ObservableList<Book> filtered = books.filtered(b ->
                b.title.toLowerCase().contains(q) || b.author.toLowerCase().contains(q));
        table.setItems(filtered);
    }

    private void loadData() {
        books.setAll(getAll());
        table.setItems(books);
    }

    private void clearFields() {
        titleField.clear(); authorField.clear(); genreField.clear(); statusBox.setValue(null);
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:booknest.db");
    }

    private static void initDB() {
        try (Connection conn = connect();
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, author TEXT, genre TEXT, status TEXT)");
        } catch (SQLException e) {}
    }

    private List<Book> getAll() {
        List<Book> list = new ArrayList<>();
        try (Connection conn = connect();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books")) {
            while (rs.next()) list.add(new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("status")));
        } catch (SQLException e) {}
        return list;
    }

    public static class Book {
        int id; String title, author, genre, status;
        public Book(int id, String t, String a, String g, String s) {
            this.id=id; this.title=t; this.author=a; this.genre=g; this.status=s;
        }
        public javafx.beans.property.SimpleStringProperty titleProperty() { return new javafx.beans.property.SimpleStringProperty(title); }
        public javafx.beans.property.SimpleStringProperty authorProperty() { return new javafx.beans.property.SimpleStringProperty(author); }
        public javafx.beans.property.SimpleStringProperty genreProperty() { return new javafx.beans.property.SimpleStringProperty(genre); }
        public javafx.beans.property.SimpleStringProperty statusProperty() { return new javafx.beans.property.SimpleStringProperty(status); }
    }
}
