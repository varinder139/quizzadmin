package com.varinder.quizzadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.UUID;

public class AddQuestionActivity extends AppCompatActivity {

    private EditText questions;
    private RadioGroup options;
    private LinearLayout answer;
    private Button uploadbtn;
    private String categoryName;
    private int  position;
    private Dialog loadingDialog;
    private QuestionModel questionModel;
    private String id, setId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_question);

        Toolbar toolbar = findViewById(R.id.tollbar_add);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Add Question");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_cornors));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        questions = findViewById(R.id.question_id);
        options = findViewById(R.id.options_id);
        answer = findViewById(R.id.answer_id);
        uploadbtn = findViewById(R.id.button_id);

        categoryName = getIntent().getStringExtra("categoryName");
        setId = getIntent().getStringExtra("setId");
        position = getIntent().getIntExtra("position", -1);
        if (setId == null) {
            finish();
            return;
        }

        if (position != -1) {
            questionModel = QuestionsActivity.list.get(position);
            setData();
        }


        uploadbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (questions.getText().toString().isEmpty()) {
                    questions.setError("Required");
                    return;
                }
                upload();
            }


        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setData() {
        questions.setText(questionModel.getQuestion());
        ((EditText) answer.getChildAt(0)).setText(questionModel.getA());
        ((EditText) answer.getChildAt(1)).setText(questionModel.getB());
        ((EditText) answer.getChildAt(2)).setText(questionModel.getC());
        ((EditText) answer.getChildAt(3)).setText(questionModel.getD());

        for (int i = 0; i < answer.getChildCount(); i++) {
            if (((EditText) answer.getChildAt(i)).getText().toString().equals(questionModel.getAnswer())) {
                RadioButton radioButton = (RadioButton) options.getChildAt(i);
                radioButton.setChecked(true);
                break;
            }
        }

    }

    private void upload() {
        int correct = -1;

        for (int i = 0; i < options.getChildCount(); i++) {

            EditText ans = (EditText) answer.getChildAt(i);
            if (ans.getText().toString().isEmpty()) {
                ans.setError("Required");
                return;
            }

            RadioButton radioButton = (RadioButton) options.getChildAt(i);
            if (radioButton.isChecked()) {
                correct = i;
                break;
            }
        }

        if (correct == -1) {
            Toast.makeText(this, "Please Mark correct option!", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("correctANS", ((EditText) answer.getChildAt(correct)).getText().toString());
        map.put("optionD", ((EditText) answer.getChildAt(3)).getText().toString());
        map.put("optionC", ((EditText) answer.getChildAt(2)).getText().toString());
        map.put("optionB", ((EditText) answer.getChildAt(1)).getText().toString());
        map.put("optionA", ((EditText) answer.getChildAt(0)).getText().toString());
        map.put("question", questions.getText().toString());
        map.put("setId", setId);

        if (position != -1) {
            id = questionModel.getId();
        } else {

             id = UUID.randomUUID().toString();
        }
        loadingDialog.show();
        FirebaseDatabase.getInstance().getReference()
                .child("SETS").child(setId).child(id)
                .setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    QuestionModel questionModel = new QuestionModel(id
                            , map.get("question").toString()
                            , map.get("optionA").toString()
                            , map.get("optionB").toString()
                            , map.get("optionC").toString()
                            , map.get("optionD").toString()
                            , map.get("correctANS").toString()
                            , map.get("setId").toString());

                    if (position != -1){
                        QuestionsActivity.list.set(position, questionModel);
                    }else {
                        QuestionsActivity.list.add(questionModel);
                    }
                    finish();
                } else {

                    Toast.makeText(AddQuestionActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
                finish();
            }
        });
    }

}