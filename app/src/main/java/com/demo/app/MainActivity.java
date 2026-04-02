package com.demo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import io.github.sceneview.SceneView;
import io.github.sceneview.node.ModelNode;

public class MainActivity extends Activity {

    private SceneView sceneView;
    private Button btnUpload, btnRecent;
    private String lastSavedModelPath = "";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Status Bar color
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.BLACK);

        // Initialize
        sceneView = findViewById(R.id.sceneView);
        btnUpload = findViewById(R.id.btnUpload);
        btnRecent = findViewById(R.id.btnRecentModel);
        prefs = getSharedPreferences("EngineData", MODE_PRIVATE);

        // Fullscreen Logic
        applyFullscreenLogic();

        // Check if previous model exists in "JSON" (SharedPreferences)
        lastSavedModelPath = prefs.getString("last_model", "");
        if (!lastSavedModelPath.isEmpty()) {
            btnRecent.setVisibility(View.VISIBLE);
        }

        // Button Click: Popup for upload
        btnUpload.setOnClickListener(v -> showUploadPopup());

        // Button Click: Load Saved Model
        btnRecent.setOnClickListener(v -> load3DModel(lastSavedModelPath));
    }

    private void showUploadPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Engine: Import Model");
        builder.setMessage("Do you want to load the default .glb model and save it to project?");
        
        builder.setPositiveButton("LOAD & SAVE", (dialog, which) -> {
            // Yahan aap file picker laga sakte hain, abhi ke liye assets use kar rahe hain
            String path = "models/engine_test.glb"; 
            saveModelToLogic(path);
            load3DModel(path);
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void saveModelToLogic(String path) {
        // SharedPreferences acts like a simple JSON store here
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_model", path);
        editor.apply();
        
        lastSavedModelPath = path;
        btnRecent.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Project Saved!", Toast.LENGTH_SHORT).show();
    }

    private void load3DModel(String path) {
        ModelNode modelNode = new ModelNode(sceneView.getEngine());
        modelNode.loadModelAsync(
            this,
            path,
            true, // auto animate
            1.0f, // scale
            model -> {
                sceneView.addChild(modelNode);
                Toast.makeText(this, "Rendering Active", Toast.LENGTH_SHORT).show();
                return null;
            }
        );
    }

    private void applyFullscreenLogic() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
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
