package io.github.ovoyo.wifitranster;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,
 Animator.AnimatorListener{

    private static final int REQ_CODE = 1024;
    private long mCurTimeMills;

    private Toolbar mToolbar;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mRecycler;
    private FloatingActionButton mFAB;

    private FileAdapter mFileAdapter;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxBus.get().register(this);
        initViews();
        setupTitle();
        setupRefresh();
        setupRecycler();
        setupData();
    }

    private void setupData(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkAndRequestPermission();
        }else {
            loadData(false);
        }
    }

    /*
    * 从 SDCard 中的 /WIFITransfer/ 中读取文件
    * */
    private void loadData(boolean refresh){

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + getString(R.string.package_name)
                    + File.separator;

            if (refresh){
                mRefresh.setRefreshing(true);
            }
            mDisposable.add(Flowable.just(dir)
                    .map((Function<String, List<File>>) s -> {
                        File dirF = new File(s);
                        boolean result = false;
                        result = dirF.exists() || dirF.mkdirs();
                        if (result) {
                            File[] files = dirF.listFiles();
                            return new ArrayList<File>(Arrays.asList(files));
                        }
                        return new ArrayList<>(0);
                    })
                    .map(files -> {
                        List<Doc> docList = new ArrayList<>();
                        if (!files.isEmpty()){
                            Doc doc;
                            for (File f: files) {
                                doc = new Doc();
                                doc.setName(f.getName());
                                doc.setPath(f.getAbsolutePath());
                                doc.setSize(f.length());
                                docList.add(doc);
                                Log.e("-----", "loadData: " + doc.toString() );
                            }
                        }
                        return docList;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(files -> {
                        if (refresh && mRefresh.isRefreshing()) {
                            mRefresh.setRefreshing(false);
                        }
                        mFileAdapter.replaceData(true,files);
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e("----", "accept: " + throwable.getMessage() );
                        }
                    }));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission(){
        List<String> lackedPermission = new ArrayList<String>();
        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (lackedPermission.isEmpty()){
            loadData(false);
        }else {
            String[] requestPermissions = new String[lackedPermission.size()];
            lackedPermission.toArray(requestPermissions);
            requestPermissions(requestPermissions, REQ_CODE);
        }
    }

    private boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE && hasAllPermissionsGranted(grantResults)) {
            loadData(false);
        } else {
            // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
            Toast.makeText(this, R.string.show_user_go_to_settings, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse(getString(R.string.package_text) + getPackageName()));
            startActivity(intent);
            finish();
        }
    }

    private void initViews(){
        mToolbar = findViewById(R.id.toolbar);
        mRefresh = findViewById(R.id.swipe_refresh_layout);
        mRecycler = findViewById(R.id.recycler_view);
        mFAB = findViewById(R.id.fab);
        mFAB.setOnClickListener(v -> {
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mFAB, "translationY", 0, mFAB.getHeight() * 2).setDuration(200L);
            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.addListener(this);
            objectAnimator.start();
        });
    }

    private void setupTitle(){
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle(R.string.app_name);
        }
    }

    private void setupRefresh(){
        mRefresh.setColorSchemeResources(R.color.colorAccent,R.color.colorPrimary);
        mRefresh.setOnRefreshListener(this);
    }

    private void setupRecycler(){
        mFileAdapter = new FileAdapter(new ArrayList<>(0));
        LinearLayoutManager layoutManager = new LinearLayoutManager(mRecycler.getContext());
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.addItemDecoration(new DividerItemDecoration(mRecycler.getContext(),layoutManager.getOrientation()));
        mRecycler.setHasFixedSize(true);
        mRecycler.setAdapter(mFileAdapter);
    }

    @Override
    public void onRefresh() {
        loadData(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            if (System.currentTimeMillis() - mCurTimeMills > 2000){
                Toast.makeText(this, R.string.click_again_to_exit, Toast.LENGTH_SHORT).show();
                mCurTimeMills = System.currentTimeMillis();
            }else {
                finish();
                System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposable.clear();
        RxBus.get().unregister(this);
    }

    @Override
    public void onAnimationStart(Animator animation) {
        WebService.start(this);
        new PopupMenuDialog(this).builder().setCancelable(false)
                .setCanceledOnTouchOutside(false).show();
    }

    @Override
    public void onAnimationEnd(Animator animation) {

    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Subscribe(tags = {@Tag(Constants.RxBusEventType.POPUP_MENU_DIALOG_SHOW_DISMISS)})
    public void onPopupMenuDialogDismiss(Integer type) {
        if (type == Constants.MSG_DIALOG_DISMISS) {
            WebService.stop(this);
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mFAB, "translationY", mFAB.getHeight() * 2, 0).setDuration(200L);
            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.start();
        }
    }
}
