package com.example.graduationproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    private Context context;
    private final OnItemClick mCallback;
    private ArrayList<Friend> FriendList;

    public FriendAdapter(Context context, OnItemClick listener) {
        this.context = context;
        this.mCallback = listener;
    }

    @NonNull
    @Override
    public FriendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendAdapter.ViewHolder holder, int position) {
        holder.onBind(FriendList.get(position));
    }

    public void setFriendList(ArrayList<Friend> list) {
        this.FriendList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return FriendList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profile;
        TextView name;
        TextView phoneNumber;
        ImageButton btn_more;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profile = itemView.findViewById(R.id.profile);
            name = itemView.findViewById(R.id.name);
            phoneNumber = itemView.findViewById(R.id.phoneNumber);

            btn_more = itemView.findViewById(R.id.ic_more);
            btn_more.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, btn_more);
                popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.modify) {
                        // 구현 아직 안 함
                        // mCallback.onClick(phoneNumber.getText().toString());
                    } else if (item.getItemId() == R.id.delete) {
                        mCallback.onClick(phoneNumber.getText().toString());
                    }

                    return false;
                });
                popupMenu.show();
            });
        }

        void onBind(Friend friend) {
            profile.setImageResource(friend.getResourceId());
            name.setText(friend.getName());
            phoneNumber.setText(friend.getPhoneNumber());
        }
    }
}
