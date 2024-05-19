package dev.upcraft.axiompatcher.control;

import com.google.gson.reflect.TypeToken;
import com.unascribed.flexver.FlexVerComparator;
import dev.upcraft.axiompatcher.Constants;
import dev.upcraft.axiompatcher.Util;
import dev.upcraft.axiompatcher.modrinth.Version;
import dev.upcraft.axiompatcher.modrinth.VersionFile;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

public class MainWindowController implements Initializable {

    private static List<Version> allVersions = List.of();
    private static final Map<String, List<Version>> modVersions = new HashMap<>();

    @FXML
    private Button btnApply;

    @FXML
    private ListView<Version> filesList;

    @FXML
    private ComboBox<String> versionSelect;

    @FXML
    private ProgressBar progress;

    @FXML
    void onClickApply(ActionEvent event) {
        var selectedVersion = filesList.getSelectionModel().getSelectedItem();
        if (selectedVersion == null) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please select a version to apply");
            alert.showAndWait();
            return;
        }

        var selectedFile = Arrays.stream(selectedVersion.files()).filter(VersionFile::primary).findFirst().orElseThrow();

        var fileChooser = new FileChooser();
        fileChooser.setTitle("Save As...");
        fileChooser.setInitialFileName(selectedFile.filename());
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(new File("."));
        var target = fileChooser.showSaveDialog(btnApply.getScene().getWindow()).toPath();
        btnApply.setVisible(false);
        progress.setVisible(true);
        progress.setProgress(0.0D);

        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        var request = HttpRequest.newBuilder(URI.create(selectedFile.url())).GET().header("User-Agent", Constants.USER_AGENT).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenAccept(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Failed to download version");
                    alert.setContentText("Received Status code %s for %s".formatted(response.statusCode(), selectedFile.url()));
                    alert.showAndWait();
                });

                return;
            }

            Path tmpFile;
            try {
                tmpFile = Files.createTempFile("axiom-patcher", ".tmp");
            } catch (IOException e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Failed to create temporary file");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });

                return;
            }

            try (InputStream in = response.body()) {
                Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                progress.setProgress(0.4D);
            } catch (IOException e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Failed to save version");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });

                return;
            }

            try {
                Util.patchFile(tmpFile, target, value -> Platform.runLater(() -> progress.setProgress(0.4D + value * 0.6D)));
                Files.deleteIfExists(tmpFile);

                Platform.runLater(() -> {
                    progress.setProgress(1.0D);
                    var alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Patch successful");
                    alert.setContentText("The version has been downloaded and patched successfully");
                    alert.showAndWait();
                });

            } catch (IOException e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText("Failed to patch version");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).thenRun(() -> Platform.runLater(() -> {
            // clean up state
            progress.setVisible(false);
            progress.setProgress(-1);
            btnApply.setVisible(true);
        }));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        var request = HttpRequest.newBuilder(Constants.LIST_AXIOM_VERSIONS_ENDPOINT).GET().header("User-Agent", Constants.USER_AGENT).build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try (var reader = new InputStreamReader(response.body())) {
                    var type = new TypeToken<Version[]>() {
                    }.getType();

                    Version[] availableVersions = Util.GSON.fromJson(reader, type);

                    allVersions = Arrays.stream(availableVersions).filter(v -> {
                        var loaders = Arrays.asList(v.loaders());
                        return loaders.contains("fabric") || loaders.contains("quilt");
                    }).filter(v -> Arrays.stream(v.files()).anyMatch(VersionFile::primary)).sorted(Comparator.comparing(Version::featured).thenComparing(v -> Instant.parse(v.date_published())).reversed()).toList();

                }
            } else {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to fetch versions from Modrinth");
                alert.showAndWait();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Failed to read versions from Modrinth");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }

        modVersions.clear();
        allVersions.forEach(v -> {
            for (String gameVersion : v.game_versions()) {
                modVersions.computeIfAbsent(gameVersion, k -> new ArrayList<>()).add(v);
            }
        });

        versionSelect.getItems().addAll(modVersions.keySet());
        versionSelect.getItems().sort(((Comparator<String>) FlexVerComparator::compare).reversed());
        versionSelect.getItems().sort(((Comparator<String>) FlexVerComparator::compare).reversed());

        filesList.getItems().clear();
        filesList.getItems().addAll(allVersions);
        if (!filesList.getItems().isEmpty()) {
            filesList.getSelectionModel().select(0);
        }

        versionSelect.valueProperty().addListener((value, oldValue, newValue) -> {
            filesList.getItems().clear();
            filesList.getItems().addAll(modVersions.getOrDefault(newValue, List.of()));

            if (!filesList.getItems().isEmpty()) {
                filesList.getSelectionModel().select(0);
            }
        });
    }

}
