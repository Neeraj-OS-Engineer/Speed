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

// Note: ComponentActivity use kar rahe hain modern File Picker ke liye
public class MainActivity extends ComponentActivity {

    private SceneView sceneView;
    private Button btnUpload, btnRecent;
    private SharedPreferences prefs;
    private ModelNode currentModelNode;

    // 1. File Picker Logic
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        saveAndLoadModel(fileUri.toString());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Setup
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.BLACK);

        sceneView = findViewById(R.id.sceneView);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecent = findViewById(R.id.btnRecentModel);
        prefs = getSharedPreferences("EngineData", MODE_PRIVATE);

        applyFullscreenLogic();

        // 2. Auto-Load System (JSON/Pref logic)
        String savedUri = prefs.getString("last_model_uri", "");
        if (!savedUri.isEmpty()) {
            btnRecent.setVisibility(View.VISIBLE);
            btnRecent.setOnClickListener(v -> load3DModel(savedUri));
            // Chahein to yahan auto-load bhi kar sakte hain bina click kiye
            load3DModel(savedUri); 
        }

        // Open Storage to pick .glb
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // Saari files dikhayega, user .glb select kare
            filePickerLauncher.launch(intent);
        });
    }

    private void saveAndLoadModel(String uriString) {
        // Save to "JSON" (Persistent Storage)
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_model_uri", uriString);
        editor.apply();

        btnRecent.setVisibility(View.VISIBLE);
        load3DModel(uriString);
    }

    private void load3DModel(String uriString) {
        if (currentModelNode != null) {
            sceneView.removeChild(currentModelNode);
        }

        Toast.makeText(this, "Loading Engine Model...", Toast.LENGTH_SHORT).show();
        
        currentModelNode = new ModelNode(sceneView.getEngine());
        currentModelNode.loadModelAsync(
                this,
                Uri.parse(uriString),
                null, // lifecycle
                true, // autoAnimate
                0.5f, // scale
                null, // center
                model -> {
                    sceneView.addChild(currentModelNode);
                    Toast.makeText(this, "Rendering Active", Toast.LENGTH_SHORT).show();
                    return null;
                }
        );
    }

    private void applyFullscreenLogic() {
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
        if (hasFocus) applyFullscreenLogic();
    }
}
