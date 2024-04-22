package com.example.graduationproject;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private static final String TAG = "FriendsAdapter"; // 로그를 위한 태그
    private List<Friend> friendsList; // 친구 목록 데이터

    // ViewHolder 클래스 정의
    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        public TextView friendNameTextView; // 친구 이름을 표시할 TextView

        public FriendViewHolder(View itemView) {
            super(itemView);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView); // 레이아웃에 friendNameTextView가 있다고 가정
        }
    }

    // 어댑터 생성자
    public FriendsAdapter(List<Friend> friendsList) {
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 아이템 뷰 생성 및 뷰홀더 초기화
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item, parent, false);
        return new FriendViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        // 특정 위치의 데이터를 뷰홀더에 바인딩
        Friend friend = friendsList.get(position);
        holder.friendNameTextView.setText(friend.getName());
        Log.d(TAG, "onBindViewHolder: " + friend.getName() + " at position " + position); // 로그 출력
    }

    @Override
    public int getItemCount() {
        // 데이터 셋 크기 반환
        return friendsList.size();
    }
}
