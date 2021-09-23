package com.varinder.quizzadmin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.UUID;

public class SetsActivity extends AppCompatActivity {
    private GridView gridView;
    private Dialog loadingDialog;
    private GridAdaptor adaptor;
    private String categoryName;
    private DatabaseReference myRef;
    private List<String> sets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        Toolbar toolbar = findViewById(R.id.toolbar_sets);
        setSupportActionBar(toolbar);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_cornors));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        categoryName = getIntent().getStringExtra("title");
        getSupportActionBar().setTitle(categoryName);

        gridView = findViewById(R.id.gridview);
        myRef = FirebaseDatabase.getInstance().getReference();

        sets = CategoryActivity.list.get(getIntent().getIntExtra("position", 0)).getSets();
         adaptor = new GridAdaptor(sets, getIntent().getStringExtra("title"), new GridAdaptor.GridListener() {
            @Override
            public void addset() {
                loadingDialog.show();

                String id = UUID.randomUUID().toString();

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                database.getReference().child("Categories").child(getIntent().getStringExtra("key")).child("sets").child(id).setValue("SET ID").addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            //adaptor.sets++;
                            sets.add(id);
                            adaptor.notifyDataSetChanged();
                        }else {
                            Toast.makeText(SetsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                        }
                        loadingDialog.dismiss();
                        adaptor.notifyDataSetChanged();
                    }
                });

            }

             @Override
             public void onLongClick(String setId, int position) {

                 new AlertDialog.Builder(SetsActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                         .setTitle("Delete SET "+position)
                         .setMessage("Are you Sure! you want to delete this Sets.")
                         .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialogInterface, int i) {

                                 loadingDialog.show();
                                 myRef.child("SETS").child(setId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                     @Override
                                     public void onComplete(@NonNull Task<Void> task) {
                                         if (task.isSuccessful()) {
                                             myRef.child("Categories").child(CategoryActivity.list.get(getIntent().getIntExtra("position", 0)).getKey())
                                                     .child("sets").child(setId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                 @Override
                                                 public void onComplete(@NonNull Task<Void> task) {
                                                     if (task.isSuccessful()){
                                                         sets.remove(setId);
                                                         adaptor.notifyDataSetChanged();
                                                     }else{
                                                         Toast.makeText(SetsActivity.this, "Somthing went wrong!", Toast.LENGTH_SHORT).show();
                                                     }
                                                     loadingDialog.dismiss();
                                                 }
                                             });

                                         } else {
                                             Toast.makeText(SetsActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                             loadingDialog.dismiss();
                                         }
                                     }
                                 });


                             }
                         })
                         .setNegativeButton("Cancel", null)
                         .setIcon(android.R.drawable.ic_dialog_alert)
                         .show();

             }
         });
        gridView.setAdapter(adaptor);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}