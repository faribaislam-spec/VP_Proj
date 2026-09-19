package com.example.rungirlrun.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rungirlrun.R;
import com.example.rungirlrun.adapters.SafetyTipAdapter;
import com.example.rungirlrun.models.SafetyTip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.widget.Switch;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.Button;
public class SafetyTipsActivity extends AppCompatActivity {

    private SafetyTipAdapter adapter;
    private List<SafetyTip> allTips = new ArrayList<>();
    private String selectedCategory = "All";
    private String selectedTime = "All";
    private boolean showingMyTips = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_tips);
        Button btnBackHome = findViewById(R.id.btnBackHome);

        btnBackHome.setOnClickListener(v -> {
            finish();
        });


        RecyclerView recyclerView = findViewById(R.id.rvSafetyTips);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String currentUserId = null;
        if (user != null) {
            currentUserId = user.getUid();
        }

        adapter = new SafetyTipAdapter(
                currentUserId,
                new SafetyTipAdapter.OnTipActionListener() {


                    @Override
                    public void onEdit(SafetyTip tip) {

                        showEditDialog(tip);

                    }


                    @Override

                    public void onDelete(SafetyTip tip) {

                        deleteTip(tip);

                    }

                }
        );
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddTip);
        Button btnMyTips = findViewById(R.id.btnMyTips);

        btnMyTips.setOnClickListener(v -> {

            showingMyTips = !showingMyTips;

            if (showingMyTips) {
                btnMyTips.setText("All Tips");
                loadMyTips();
            } else {
                btnMyTips.setText("My Tips");
                loadTips();
            }

        });
        Spinner spinnerFilterCategory = findViewById(R.id.spinnerFilterCategory);

        String[] categories = {
                "All",
                "Personal Safety",
                "Travel Safety",
                "Digital Safety",
                "Emergency",
                "Home Safety",
                "Community"
        };


        ArrayAdapter<String> filterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                );

        spinnerFilterCategory.setAdapter(filterAdapter);
        loadTips();
        //filter tips according to category
        spinnerFilterCategory.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedCategory = categories[position];
                        filterTips();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {

                    }
                });
        fab.setOnClickListener(v -> showAddTipDialog());
        MaterialSwitch switchNight = findViewById(R.id.switchNight);
        //night and day toggle
        switchNight.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                selectedTime = "Night";
            } else {
                selectedTime = "Day";
            }

            filterTips();

        });
        //Only once for testing after new integration
        //addDefaultTips();

    }
    //load tips present in firebase
    private void loadTips() {

        FirebaseFirestore.getInstance()
                .collection("safety_tips")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null)
                        return;

                    List<SafetyTip> tipList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {

                        SafetyTip tip = doc.toObject(SafetyTip.class);
                        tip.setId(doc.getId());
                        tipList.add(tip);
                    }

                    allTips = tipList;
                    filterTips();
                });
    }

    private void loadMyTips() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null)
            return;

        FirebaseFirestore.getInstance()
                .collection("safety_tips")
                .whereEqualTo("authorId", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null)
                        return;

                    List<SafetyTip> myTips = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {

                        SafetyTip tip = doc.toObject(SafetyTip.class);
                        tip.setId(doc.getId());
                        myTips.add(tip);
                    }

                    allTips = myTips;
                    filterTips();
                });
    }
    //when adding tips
    private void showAddTipDialog() {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_tip, null);

        EditText etTip = dialogView.findViewById(R.id.etTipInput);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        MaterialSwitch switchTipNight =
                dialogView.findViewById(R.id.switchTipNight);

        String[] categories = {
                "Personal Safety",
                "Travel Safety",
                "Digital Safety",
                "Emergency",
                "Home Safety",
                "Other"
        };

        //show category dropdown
        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                );

        spinnerCategory.setAdapter(categoryAdapter);


        new AlertDialog.Builder(this)
                .setTitle("Add a Safety Tip")
                .setView(dialogView)

                .setPositiveButton("Post", (dialog, which) -> {

                    String text = etTip.getText()
                            .toString()
                            .trim();

                    String category =
                            spinnerCategory.getSelectedItem().toString();
                    String time;

                    if (switchTipNight.isChecked()) {
                        time = "Night";
                    } else {
                        time = "Day";
                    }

                    if (!text.isEmpty()) {
                        submitTip(text, category, time);
                    }

                })

                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitTip(String tipText, String category, String time) {

        Map<String, Object> tip = new HashMap<>();

        tip.put("tipText", tipText);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        tip.put("authorId", user.getUid());

        String name = user.getDisplayName();

        if (name == null || name.trim().isEmpty()) {
            name = user.getEmail();
        }

        tip.put("authorName", name);
        tip.put("isOfficial", false);
        tip.put("timestamp", FieldValue.serverTimestamp());
        tip.put("category", category);
        tip.put("time", time);
        FirebaseFirestore.getInstance()
                .collection("safety_tips")
                .add(tip)
                .addOnSuccessListener(docRef ->
                        Toast.makeText(this, "Tip added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    //for initialization
    private void addDefaultTips() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String[][] tips = {

                {"Always share your live location with trusted contacts.",
                        "Travel Safety",
                        "Night"},

                {"Avoid walking alone in isolated areas.",
                        "Personal Safety",
                        "Night"},

                {"Keep emergency contacts updated.",
                        "Emergency",
                        "Day"},

                {"Stay aware of your surroundings when outside.",
                        "Personal Safety",
                        "Day"},

                {"Move to a crowded area if you feel unsafe.",
                        "Emergency",
                        "Night"}
        };


        for (String[] tipData : tips) {

            Map<String, Object> tip = new HashMap<>();

            tip.put("tipText", tipData[0]);
            tip.put("category", tipData[1]);
            tip.put("time", tipData[2]);
            tip.put("authorId", "app");
            tip.put("authorName", "RunGirlRun Team");
            tip.put("isOfficial", true);
            tip.put("timestamp", FieldValue.serverTimestamp());


            db.collection("safety_tips")
                    .add(tip);
        }

        Toast.makeText(this,
                "Default tips added!",
                Toast.LENGTH_SHORT).show();
    }

    private void filterTips() {

        List<SafetyTip> filtered = new ArrayList<>();

        for (SafetyTip tip : allTips) {

            boolean categoryMatch =
                    selectedCategory.equals("All") ||
                            selectedCategory.equals(tip.getCategory());


            boolean timeMatch =
                    selectedTime.equals("All") ||
                            selectedTime.equals(tip.getTime());


            if (categoryMatch && timeMatch) {
                filtered.add(tip);
            }
        }

        adapter.setTips(filtered);
    }
    //delete tip
    private void deleteTip(SafetyTip tip){

        new AlertDialog.Builder(this)
                .setTitle("Delete Tip?")
                .setMessage("Are you sure you want to delete this tip?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    FirebaseFirestore.getInstance()
                            .collection("safety_tips")
                            .document(tip.getId())
                            .delete()
                            .addOnSuccessListener(v -> {

                                Toast.makeText(
                                        this,
                                        "Tip deleted",
                                        Toast.LENGTH_SHORT
                                ).show();

                            });

                })
                .setNegativeButton("Cancel", null)
                .show();

    }


    //show edit
    private void showEditDialog(SafetyTip tip) {

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_tip, null);


        EditText etTip = view.findViewById(R.id.etTipInput);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerCategory);
        MaterialSwitch switchTipNight =
                view.findViewById(R.id.switchTipNight);


        etTip.setText(tip.getTipText());


        String[] categories = {
                "Personal Safety",
                "Travel Safety",
                "Digital Safety",
                "Emergency",
                "Home Safety",
                "Other"
        };


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                );

        spinnerCategory.setAdapter(adapter);


        new AlertDialog.Builder(this)
                .setTitle("Edit Safety Tip")
                .setView(view)

                .setPositiveButton("Save", (dialog, which) -> {


                    Map<String, Object> updates = new HashMap<>();

                    updates.put(
                            "tipText",
                            etTip.getText().toString()
                    );

                    updates.put(
                            "category",
                            spinnerCategory.getSelectedItem().toString()
                    );


                    updates.put(
                            "time",
                            switchTipNight.isChecked()
                                    ? "Night"
                                    : "Day"
                    );


                    FirebaseFirestore.getInstance()
                            .collection("safety_tips")
                            .document(tip.getId())
                            .update(updates);


                })

                .setNegativeButton("Cancel", null)
                .show();

    }
}


