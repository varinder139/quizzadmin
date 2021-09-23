package com.varinder.quizzadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoryAdaptor extends RecyclerView.Adapter<CategoryAdaptor.ViewHolder> {

    private List<categoryModle> categoryModleList;
    private DeleteListener deleteListener;

    public CategoryAdaptor(List<categoryModle> categoryModleList, DeleteListener deleteListener) {
        this.categoryModleList = categoryModleList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent, false);
        return  new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String image = categoryModleList.get(position).getUrl();
        String mainTitle = categoryModleList.get(position).getName();
       // int getSets = categoryModleList.get(position).getSets();
        String key = categoryModleList.get(position).getKey();

        holder.setData(image, mainTitle, key, position);
    }

    @Override
    public int getItemCount() {
        return categoryModleList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private CircleImageView imageView;
        private TextView title;
        private ImageButton delete;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageview);
            title = itemView.findViewById(R.id.title);
            delete = itemView.findViewById(R.id.delete);


        }

        private void setData(String url, final String title, final String key, final int position){
            Glide.with(itemView.getContext()).load(url).into(imageView);
            this.title.setText(title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(itemView.getContext(), SetsActivity.class);
                    intent.putExtra("title", title);
                    intent.putExtra("position", position);
                    intent.putExtra("key", key);

                    itemView.getContext().startActivity(intent);
                }
            });

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteListener.onDelete(key, position);
                }
            });

        }
    }
    public interface DeleteListener{

        public void onDelete(String key, int position);
    }
}
