package com.demo.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import io.github.sceneview.SceneView;
import io.github.sceneview.node.ModelNode;

public class MainActivity extends ComponentActivity {

    private SceneView sceneView;
    private Button btnUpload, btnRecent;
    private SharedPreferences prefs;
    private ModelNode currentModelNode;

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        saveAndRender(uri.toString());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupEngineUI();
        applyFullscreen();

        sceneView = findViewById(R.id.sceneView);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecent = findViewById(R.id.btnRecentModel);
        prefs = getSharedPreferences("SpeedEngineData", MODE_PRIVATE);

        String lastModelUri = prefs.getString("last_model_uri", "");
        if (!lastModelUri.isEmpty()) {
            btnRecent.setVisibility(View.VISIBLE);
            btnRecent.setOnClickListener(v -> renderModel(lastModelUri));
            renderModel(lastModelUri);
        } else {
            btnRecent.setVisibility(View.GONE);
        }

        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("model/gltf-binary");
            pickerLauncher.launch(intent);
        });
    }

    private void saveAndRender(String uriPath) {
        prefs.edit().putString("last_model_uri", uriPath).apply();
        btnRecent.setVisibility(View.VISIBLE);
        renderModel(uriPath);
    }

    private void renderModel(String uriPath) {
        // Clean up previous model – destroy() is the correct method in 3.6.0
        if (currentModelNode != null) {
            sceneView.removeChild(currentModelNode);
            currentModelNode.destroy();   // ✅ valid in SceneView 3.6.0
        }

        Toast.makeText(this, "Engine: Loading 3D Model...", Toast.LENGTH_SHORT).show();

        Uri modelUri = Uri.parse(uriPath);
        currentModelNode = new ModelNode(sceneView.getEngine());

        // ✅ Correct API for SceneView 3.6.0: loadModel() with a listener (Java-friendly)
        currentModelNode.loadModel(
                this,          // Context
                modelUri,      // Uri
                true,          // autoAnimate
                0.5f,          // scale
                null,          // center position
                new ModelNode.LoadModelListener() {
                    @Override
                    public void onSuccess(ModelNode model) {
                        sceneView.addChild(currentModelNode);
                        Toast.makeText(MainActivity.this, "Rendering Active", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable error) {
                        Toast.makeText(MainActivity.this, "Failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        currentModelNode = null;
                    }
                }
        );
    }

    private void setupEngineUI() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.BLACK);
    }

    private void applyFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyFullscreen();
    }
}
