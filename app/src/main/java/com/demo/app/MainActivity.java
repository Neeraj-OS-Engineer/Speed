package com.demo.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.sceneview.SceneView;
import io.github.sceneview.node.ModelNode;
import kotlin.Unit;

public class MainActivity extends ComponentActivity {

    private SceneView sceneView;
    private DrawerLayout drawerLayout;
    private LinearLayout modelsListContainer;
    private TextView txtStatus;
    private SharedPreferences prefs;
    private ModelNode currentModelNode;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        takePersistableUriPermission(uri);
                        saveModelUri(uri.toString());
                        renderModel(uri.toString());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sceneView = findViewById(R.id.sceneView);
        drawerLayout = findViewById(R.id.drawerLayout);
        modelsListContainer = findViewById(R.id.modelsListContainer);
        txtStatus = findViewById(R.id.txtStatus);
        prefs = getSharedPreferences("SpeedEngineData", MODE_PRIVATE);

        Button btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        Button btnImportFile = findViewById(R.id.btnImportFile);
        Button btnDownloadUrl = findViewById(R.id.btnDownloadUrl);

        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawerContent)));
        btnImportFile.setOnClickListener(v -> openFilePicker());
        btnDownloadUrl.setOnClickListener(v -> showDownloadDialog());

        refreshModelsList();

        // Auto-load last used model
        String lastModel = prefs.getString("last_model_uri", "");
        if (!lastModel.isEmpty()) {
            renderModel(lastModel);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("model/gltf-binary");
        filePickerLauncher.launch(intent);
    }

    private void takePersistableUriPermission(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Ignore if permission not needed
        }
    }

    private void saveModelUri(String uriString) {
        Set<String> uris = new HashSet<>(prefs.getStringSet("model_uris", new HashSet<>()));
        uris.add(uriString);
        prefs.edit().putStringSet("model_uris", uris).apply();
        prefs.edit().putString("last_model_uri", uriString).apply();
        refreshModelsList();
    }

    private void refreshModelsList() {
        modelsListContainer.removeAllViews();
        Set<String> uris = prefs.getStringSet("model_uris", new HashSet<>());
        if (uris.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No models saved.\nImport or download one.");
            emptyText.setTextColor(Color.GRAY);
            modelsListContainer.addView(emptyText);
            return;
        }

        for (String uriString : uris) {
            MaterialButton modelButton = new MaterialButton(this);
            String displayName = getDisplayName(uriString);
            modelButton.setText(displayName);
            modelButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            modelButton.setBackgroundColor(Color.parseColor("#444444"));
            modelButton.setTextColor(Color.WHITE);
            modelButton.setOnClickListener(v -> renderModel(uriString));
            modelButton.setOnLongClickListener(v -> {
                removeModel(uriString);
                return true;
            });
            modelsListContainer.addView(modelButton);
        }
    }

    private String getDisplayName(String uriString) {
        if (uriString.startsWith("content://")) {
            String name = uriString.substring(uriString.lastIndexOf("/") + 1);
            return name.length() > 20 ? name.substring(0, 20) + "..." : name;
        } else if (uriString.startsWith("file://")) {
            File f = new File(uriString.replace("file://", ""));
            return f.getName();
        } else {
            return "Model: " + uriString.substring(0, Math.min(uriString.length(), 20));
        }
    }

    private void removeModel(String uriString) {
        Set<String> uris = new HashSet<>(prefs.getStringSet("model_uris", new HashSet<>()));
        uris.remove(uriString);
        prefs.edit().putStringSet("model_uris", uris).apply();
        String last = prefs.getString("last_model_uri", "");
        if (last.equals(uriString)) {
            prefs.edit().remove("last_model_uri").apply();
            if (currentModelNode != null) {
                sceneView.removeChild(currentModelNode);
                currentModelNode.destroy();
                currentModelNode = null;
            }
        }
        refreshModelsList();
        Toast.makeText(this, "Model removed", Toast.LENGTH_SHORT).show();
    }

    private void showDownloadDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_download);
        EditText etUrl = dialog.findViewById(R.id.etUrl);
        Button btnDownload = dialog.findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                dialog.dismiss();
                downloadAndSaveModel(url);
            } else {
                Toast.makeText(this, "Enter URL", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void downloadAndSaveModel(String urlString) {
        txtStatus.setText("Downloading...");
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.connect();
                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() -> txtStatus.setText("Download failed: " + conn.getResponseCode()));
                    return;
                }
                String fileName = URLUtil.guessFileName(urlString, null, null);
                File cacheDir = getExternalCacheDir();
                File destFile = new File(cacheDir, fileName);
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                }
                Uri localUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", destFile);
                runOnUiThread(() -> {
                    saveModelUri(localUri.toString());
                    renderModel(localUri.toString());
                    txtStatus.setText("Download complete");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    txtStatus.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Download failed", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void renderModel(String uriPath) {
        if (currentModelNode != null) {
            sceneView.removeChild(currentModelNode);
            currentModelNode.destroy();
        }

        txtStatus.setText("Loading model...");
        Uri modelUri = Uri.parse(uriPath);
        currentModelNode = new ModelNode(sceneView.getEngine());

        // ✅ Correct call for SceneView 3.6.0 (Kotlin -> Java interop)
        currentModelNode.loadModelAsync(
                this,
                modelUri,
                true,   // autoAnimate
                0.5f,   // scale
                null,   // center
                model -> {
                    sceneView.addChild(currentModelNode);
                    runOnUiThread(() -> txtStatus.setText("Model ready"));
                    return Unit.INSTANCE;  // CRITICAL: return Unit for Kotlin
                },
                error -> {
                    runOnUiThread(() -> {
                        txtStatus.setText("Error: " + error.getMessage());
                        Toast.makeText(this, "Failed to load model", Toast.LENGTH_LONG).show();
                    });
                    currentModelNode = null;
                    return Unit.INSTANCE;
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (currentModelNode != null) {
            currentModelNode.destroy();
        }
        executor.shutdown();
        super.onDestroy();
    }
}
