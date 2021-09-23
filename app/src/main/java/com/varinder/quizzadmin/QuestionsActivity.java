package com.varinder.quizzadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.AsyncTaskLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public class QuestionsActivity extends AppCompatActivity {

    private Button add, excel;
    private RecyclerView recyclerView;
    private QuestionsAdaptor adaptor;
    public static List<QuestionModel> list;
    private Dialog loadingDialog;
    private DatabaseReference myRef;
    private TextView loadingText;
    private String setId;
    private String categoryName;
    public static final int CELL_COUNT = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myRef = FirebaseDatabase.getInstance().getReference();

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_cornors));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        loadingText = loadingDialog.findViewById(R.id.textView4);

        categoryName = getIntent().getStringExtra("category");
        setId = getIntent().getStringExtra("setId");
        getSupportActionBar().setTitle(categoryName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        add = findViewById(R.id.add_btn);
        excel = findViewById(R.id.excel_btn);
        recyclerView = findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();
        adaptor = new QuestionsAdaptor(list, categoryName, new QuestionsAdaptor.DeleteListener() {
            @Override
            public void onLongClick(int position, String id) {
                ///Alert Dialog Start here

                new AlertDialog.Builder(QuestionsActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Questions")
                        .setMessage("Are you Sure! you want to delete this Question.")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                loadingDialog.show();
                                myRef.child("SETS").child(setId).child(id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {

                                            list.remove(position);
                                            adaptor.notifyItemRemoved(position);

                                        } else {
                                            Toast.makeText(QuestionsActivity.this, "Failed to Delete ", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();


                /////////////////Alert End here

            }
        });
        recyclerView.setAdapter(adaptor);

        getData(categoryName, setId);

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addQuestions = new Intent(QuestionsActivity.this, AddQuestionActivity.class);
                addQuestions.putExtra("categoryName", categoryName);
                addQuestions.putExtra("setId", setId);
                startActivity(addQuestions);
            }
        });

        //here cliked on the the buttion after that check the permission code
        excel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(QuestionsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // here call the function or task
                    selectFile();
                } else {
                    ActivityCompat.requestPermissions(QuestionsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // here also call the function
                selectFile();
            } else {
                Toast.makeText(this, "Please Grant Permissions!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectFile() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), 102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102) {
            if (requestCode == RESULT_OK) {
                String filePath = data.getData().getPath();
                if (filePath.endsWith(".xlsx")) {
                    readFile(data.getData());
                    //Toast.makeText(this, "File Selected", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Please choose an Excel file!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getData(String categoryName, String setId) {
        loadingDialog.show();
        //FirebaseDatabase.getInstance().getReference()
        myRef.child("SETS").child(setId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            String id = dataSnapshot.getKey();
                            String qut = dataSnapshot.child("question").getValue().toString();
                            String a = dataSnapshot.child("optionA").getValue().toString();
                            String b = dataSnapshot.child("optionB").getValue().toString();
                            String c = dataSnapshot.child("optionC").getValue().toString();
                            String d = dataSnapshot.child("optionD").getValue().toString();
                            String correctAns = dataSnapshot.child("correctANS").getValue().toString();

                            list.add(new QuestionModel(id, qut, a, b, c, d, correctAns, setId));

                        }
                        loadingDialog.dismiss();
                        adaptor.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(QuestionsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                        finish();
                    }
                });
    }

    private void readFile(Uri fileUri) {
        loadingText.setText("Scanning Questions....");
        loadingDialog.show();
       // File file = new File(filePath);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {



        HashMap<String, Object> parentMap = new HashMap<>();
        List<QuestionModel> tempList = new ArrayList<>();

        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

            int rowsCount = sheet.getPhysicalNumberOfRows();

            if (rowsCount > 0 ){
                for (int r = 0; r < rowsCount; r++){
                    Row row = sheet.getRow(r);

                    if (row.getPhysicalNumberOfCells() == CELL_COUNT) {
                        String question = getCellData(row, 0, formulaEvaluator);
                        String a = getCellData(row, 1, formulaEvaluator);
                        String b = getCellData(row, 2, formulaEvaluator);
                        String c = getCellData(row, 3, formulaEvaluator);
                        String d = getCellData(row, 4, formulaEvaluator);
                        String correctAns = getCellData(row, 5, formulaEvaluator);

                        if (correctAns.equals(a) || correctAns.equals(b) || correctAns.equals(c) || correctAns.equals(d)){

                            HashMap<String, Object> questionMap = new HashMap<>();
                            questionMap.put("question", question);
                            questionMap.put("optionA", a);
                            questionMap.put("optionB", b);
                            questionMap.put("optionC", c);
                            questionMap.put("optionD", d);
                            questionMap.put("correctANS", correctAns);
                            questionMap.put("setId", setId);

                            String id = UUID.randomUUID().toString();
                            parentMap.put(id, questionMap);

                            tempList.add(new QuestionModel(id, question, a,b,c,d, correctAns, setId));

                        }else {
                            int finalR1 = r;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadingText.setText("Loading...");
                                    loadingDialog.dismiss();
                                    Toast.makeText(QuestionsActivity.this, "Row No. "+(finalR1 +1)+" has no correct option", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                    }else{
                        int finalR = r;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingText.setText("Loading...");
                                loadingDialog.dismiss();
                                Toast.makeText(QuestionsActivity.this, "Row No. "+(finalR +1)+" has incorret data", Toast.LENGTH_SHORT).show();

                            }
                        });
                        return;
                    }
                }

                ///after sucessfully run the for loop we will set the firebase querry

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingText.setText("Uploading...");

                        FirebaseDatabase.getInstance().getReference()
                                .child("SETS").child(setId).updateChildren(parentMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    list.addAll(tempList);
                                    adaptor.notifyDataSetChanged();
                                }else {
                                    loadingText.setText("Loading...");
                                    Toast.makeText(QuestionsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                }
                                loadingDialog.dismiss();

                            }
                        });

                    }
                });


            }else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingText.setText("Loading...");
                        loadingDialog.dismiss();
                        Toast.makeText(QuestionsActivity.this, "File is Empty!", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingText.setText("Loading...");
                    loadingDialog.dismiss();
                    Toast.makeText(QuestionsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadingText.setText("Loading...");
                    loadingDialog.dismiss();
                    Toast.makeText(QuestionsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adaptor.notifyDataSetChanged();
    }

    private String getCellData(Row row, int cellPosition, FormulaEvaluator formulaEvaluator){

        String value = "";
        Cell cell = row.getCell(cellPosition);

        switch (cell.getCellType()){
            case Cell.CELL_TYPE_BOOLEAN:
                return value+cell.getBooleanCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                return value+cell.getNumericCellValue();

            case Cell.CELL_TYPE_STRING:
                return value+cell.getStringCellValue();
            case Cell.CELL_TYPE_FORMULA:
                return  value+cell.getCellFormula();

            default:
                return value;
        }
    }
}