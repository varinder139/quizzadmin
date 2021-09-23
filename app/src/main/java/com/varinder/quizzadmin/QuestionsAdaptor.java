package com.varinder.quizzadmin;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuestionsAdaptor extends RecyclerView.Adapter<QuestionsAdaptor.ViewHolderB> {

    private List<QuestionModel> list;
    private String category;
    private DeleteListener listener;

    public QuestionsAdaptor(List<QuestionModel> list, String category, DeleteListener listener) {

        this.list = list;
        this.category = category;
        this.listener = listener;
    }


    @NonNull
    @Override
    public ViewHolderB onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.question_item, parent, false);
        return new ViewHolderB(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderB holder, int position) {
        String question = list.get(position).getQuestion();
        String answer = list.get(position).getAnswer();

        holder.setData(question, answer, position);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolderB extends RecyclerView.ViewHolder{

        private TextView question, answer;

        public ViewHolderB(@NonNull View itemView) {
            super(itemView);

            question = itemView.findViewById(R.id.questiond);
            answer = itemView.findViewById(R.id.answer);
        }

        private void setData(String qustions, String answers, int position){
            this.question.setText(position+1+". "+qustions);
            this.answer.setText("Ans. "+answers);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(itemView.getContext(), AddQuestionActivity.class);
                    intent.putExtra("categoryName", category);
                    intent.putExtra("setId", list.get(position).getSet());
                    intent.putExtra("position", position);
                    itemView.getContext().startActivity(intent);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    listener.onLongClick(position, list.get(position).getId());
                    return false;
                }
            });

        }
    }
    public interface DeleteListener{
        void onLongClick(int position, String id);
    }
}
