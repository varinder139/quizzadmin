package com.varinder.quizzadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    FirebaseDatabase database; // = FirebaseDatabase.getInstance();
    DatabaseReference myRef; // = database.getReference();

    private Dialog loadingDialog, categoryDialog;
    private RecyclerView recyclerView;
    public static List<categoryModle> list;

    private ImageView addImage;
    private EditText categoryNmae;
    private Button addButton;
    private Uri image;
    private String downloadUrl;
    private CategoryAdaptor adaptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);


        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_cornors));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        setCategoryDialog();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //below mentiond for set the title bar title
        getSupportActionBar().setTitle("Categories");
        //below mentioned for enable back buttion on ation bar
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        recyclerView = findViewById(R.id.rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();
        adaptor = new CategoryAdaptor(list, new CategoryAdaptor.DeleteListener() {
            @Override
            public void onDelete(String key, int position) {

                new AlertDialog.Builder(CategoryActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                        .setTitle("Delete Category")
                        .setMessage("Are you Sure! you want to delete this categories.")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                loadingDialog.show();
                                myRef.child("Categories").child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){

                                            for (String setIds : list.get(position).getSets()){
                                                myRef.child("SETS").child(setIds).removeValue();
                                            }
                                            list.remove(position);
                                            adaptor.notifyDataSetChanged();
                                            loadingDialog.dismiss();
                                        }else {
                                            Toast.makeText(CategoryActivity.this, "Failed to Delete ", Toast.LENGTH_SHORT).show();
                                        }
                                        loadingDialog.dismiss();
                                    }
                                });

                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();



            }
        });
        recyclerView.setAdapter(adaptor);

        loadingDialog.show();
        myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    List<String> sets = new ArrayList<>();
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.child("sets").getChildren()){
                        sets.add(dataSnapshot1.getKey());
                    }
                    list.add(new categoryModle(dataSnapshot.child("name").getValue().toString(),
                            sets,
                            dataSnapshot.child("url").getValue().toString(),
                            dataSnapshot.getKey()));
                }
                adaptor.notifyDataSetChanged();
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoryActivity.this, "This is error " + error.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
        //Toast.makeText(this, "hellow", Toast.LENGTH_SHORT).show();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add) {
            categoryDialog.show();
        }
        if (item.getItemId() == R.id.logout){
            new AlertDialog.Builder(CategoryActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                    .setTitle("LogOut")
                    .setMessage("Are you Sure! you want to logout?")
                    .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            loadingDialog.show();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(CategoryActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCategoryDialog() {
        categoryDialog = new Dialog(this);
        categoryDialog.setContentView(R.layout.add_categories_dialog);
        // categoryDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.rounded_cornors));
        categoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryDialog.setCancelable(true);

        addImage = categoryDialog.findViewById(R.id.image);
        categoryNmae = categoryDialog.findViewById(R.id.categoryName);
        addButton = categoryDialog.findViewById(R.id.addbtn);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // old data as are below
                /*
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
               startActivityForResult(galleryIntent, 101);
               */

                // new method added for check permission and add the image

                if (ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            CategoryActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                } else {
                    selectImage();
                }



            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (categoryNmae.getText() == null || categoryNmae.getText().toString().isEmpty()) {
                    categoryNmae.setError("Required");
                }
                for (categoryModle modle : list){
                    if (categoryNmae.getText().toString().equals(modle.getName())){
                        categoryNmae.setError("Category name Already present!");
                        return;
                    }
                }
                if (image == null) {
                    Toast.makeText(CategoryActivity.this, "Please Select Your Image", Toast.LENGTH_SHORT).show();
                    return;
                }
                categoryDialog.dismiss();

                //upload data here
                uploadData();
            }
        });

    }

    /// yo new method
    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) !=null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    //// also new
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            if (grantResults[0]  == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            //Toast.makeText(this, "yy "+requestCode, Toast.LENGTH_SHORT).show();

            if (data != null) {
                image = data.getData();
                //addImage.setImageURI(image);
           /* if (requestCode == Activity.RESULT_OK) {
                image = data.getData();
                addImage.setImageURI(image);
            }  */

                if (image != null) {
                    try {

                        InputStream inputStream = getContentResolver().openInputStream(image);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        addImage.setImageBitmap(bitmap);
                        Toast.makeText(this, "" + bitmap, Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        }
    }

    private void uploadData() {
        loadingDialog.show();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference imageReference = storageReference.child("categories").child(image.getLastPathSegment());

        //  final StorageReference ref = storageRef.child("images/mountains.jpg");
        UploadTask uploadTask = imageReference.putFile(image);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            downloadUrl = task.getResult().toString();
                            uploadCategoriesName();
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(CategoryActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(CategoryActivity.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadCategoriesName() {

        Map<String, Object> map = new HashMap<>();
        map.put("name", categoryNmae.getText().toString());
        map.put("sets", 0);
        map.put("url", downloadUrl);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        String id = UUID.randomUUID().toString();

        database.getReference().child("Categories").child(id).setValue(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    list.add(new categoryModle(categoryNmae.getText().toString(), new ArrayList<String>(), downloadUrl, id));
                    adaptor.notifyDataSetChanged();
                } else {
                    Toast.makeText(CategoryActivity.this, "error", Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }
        });
    }
}