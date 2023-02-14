package com.reodinas2.postingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.reodinas2.postingapp.adapter.PostingAdapter;
import com.reodinas2.postingapp.api.NetworkClient;
import com.reodinas2.postingapp.api.PostingApi;
import com.reodinas2.postingapp.config.Config;
import com.reodinas2.postingapp.model.Posting;
import com.reodinas2.postingapp.model.PostingList;
import com.reodinas2.postingapp.model.Res;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {
    Button btnAdd;

    RecyclerView recyclerView;
    PostingAdapter adapter;
    ArrayList<Posting> postingList = new ArrayList<>();

    ProgressBar progressBar;

    // 페이징 처리를 위한 변수
    int count = 0;
    int offset = 0;
    int limit = 7;
    private Posting selectedPosting;
    String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 억세스토큰이 저장되어 있으면,
        // 로그인한 유저이므로 메인액티비티를 실행하고,

        // 그렇지 않으면,
        // 회원가입 액티비티를 실행하고 메인액티비티는 종료!

        SharedPreferences sp = getSharedPreferences(Config.SP_NAME, MODE_PRIVATE);
        accessToken = sp.getString(Config.ACCESS_TOKEN, "");

        if (accessToken.isEmpty()){

            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 회원가입/로그인 유저면, 아래 코드를 실행하도록 둔다.

        btnAdd = findViewById(R.id.btnAdd);
        progressBar = findViewById(R.id.progressBar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int lastPosition = ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
                int totalCount = recyclerView.getAdapter().getItemCount();
                if(lastPosition+1 == totalCount){
                    // 네트워크 통해서 데이터를 더 불러온다.
                    if(count == limit){
                        addNetworkData();
                    }
                }
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddActivity.class);
                startActivity(intent);
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();

        getNetworkData();
    }


    void getNetworkData(){
        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);

        PostingApi api = retrofit.create(PostingApi.class);

        offset = 0;

        String token = "Bearer " + accessToken;

        Call<PostingList> call = api.getPosting(token, offset, limit);

        call.enqueue(new Callback<PostingList>() {
            @Override
            public void onResponse(Call<PostingList> call, Response<PostingList> response) {
                progressBar.setVisibility(View.GONE);

                if(response.isSuccessful()){

                    postingList.clear();

                    count = response.body().getCount();
                    postingList.addAll( response.body().getItems() );

                    offset = offset + count;

                    adapter = new PostingAdapter(MainActivity.this, postingList);
                    recyclerView.setAdapter(adapter);

                }



            }

            @Override
            public void onFailure(Call<PostingList> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });

    }

    private void addNetworkData() {
        progressBar.setVisibility(View.VISIBLE);

        Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);

        PostingApi api = retrofit.create(PostingApi.class);

        String token = "Bearer " + accessToken;

        Call<PostingList> call = api.getPosting(token, offset, limit);

        call.enqueue(new Callback<PostingList>() {
            @Override
            public void onResponse(Call<PostingList> call, Response<PostingList> response) {
                progressBar.setVisibility(View.GONE);

                if(response.isSuccessful()){

                    count = response.body().getCount();
                    postingList.addAll( response.body().getItems() );

                    adapter.notifyDataSetChanged();

                    offset = offset + count;

                } else{

                }

            }

            @Override
            public void onFailure(Call<PostingList> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }


    public void likeProcess(int index){

        selectedPosting = postingList.get(index);

        // 2. 해당 행의 좋아요가 이미 좋아요인지 아닌지 파악
        if (selectedPosting.getIsLike() == 0) {
            // 3. 좋아요 API를 호출
            Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);
            PostingApi api = retrofit.create(PostingApi.class);

            SharedPreferences sp = getSharedPreferences(Config.SP_NAME, Context.MODE_PRIVATE);
            String accessToken = "Bearer " + sp.getString(Config.ACCESS_TOKEN, "");

            Call<Res> call = api.setLike(accessToken, selectedPosting.getPostingId());

            call.enqueue(new Callback<Res>() {
                @Override
                public void onResponse(Call<Res> call, Response<Res> response) {
                    if (response.isSuccessful()) {
                        // 4. 화면에 결과를 표시
                        selectedPosting.setIsLike(1);
                        adapter.notifyDataSetChanged();


                    } else {

                    }
                }

                @Override
                public void onFailure(Call<Res> call, Throwable t) {

                }
            });


        } else {
            // 3. 좋아요 해제 API를 호출
            Retrofit retrofit = NetworkClient.getRetrofitClient(MainActivity.this);
            PostingApi api = retrofit.create(PostingApi.class);

            SharedPreferences sp = getSharedPreferences(Config.SP_NAME, Context.MODE_PRIVATE);
            String accessToken = "Bearer " + sp.getString(Config.ACCESS_TOKEN, "");

            Call<Res> call = api.deleteLike(accessToken, selectedPosting.getPostingId());

            call.enqueue(new Callback<Res>() {
                @Override
                public void onResponse(Call<Res> call, Response<Res> response) {
                    if (response.isSuccessful()) {
                        // 4. 화면에 결과를 표시
                        selectedPosting.setIsLike(0);
                        adapter.notifyDataSetChanged();


                    } else {

                    }
                }

                @Override
                public void onFailure(Call<Res> call, Throwable t) {

                }
            });

        }
    }



}