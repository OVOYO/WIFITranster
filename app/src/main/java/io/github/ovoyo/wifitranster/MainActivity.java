package io.github.ovoyo.wifitranster;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;

import com.hwangjr.rxbus.RxBus;
import com.hwangjr.rxbus.annotation.Subscribe;
import com.hwangjr.rxbus.annotation.Tag;
import com.hwangjr.rxbus.thread.EventThread;

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
        Animator.AnimatorListener, FileAdapter.FileAdapterClickListener {

    private static final int REQ_CODE = 1024;
    private long mCurTimeMills;

    private Toolbar mToolbar;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mRecycler;
    private FloatingActionButton mFAB;

    private FileAdapter mFileAdapter;

    private final CompositeDisposable mDisposable = new CompositeDisposable();

    private ActionMode mActionMode;
    private ActionModelCallback mActionModelCallback;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void enableActionMode(int index) {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(mActionModelCallback);
        }
        toggleSelection(index);
    }

    private void toggleSelection(int index) {
        mFileAdapter.toggleSelection(index);
        int count = mFileAdapter.getSelectedItemCount();
        if (count == 0) {
            mActionMode.finish();
            return;
        }
        mActionMode.setTitle(String.valueOf(count));
        mActionMode.invalidate();
    }

    private void deleteSelectedItems() {
        List<Integer> selectList = mFileAdapter.getSelectedItems();
        if (selectList == null || selectList.isEmpty()) {
            return;
        }
        int index = selectList.size() - 1;
        while (index >= 0) {
            mFileAdapter.removeData(selectList.get(index));
            index -= 1;
        }
        mFileAdapter.notifyDataSetChanged();
    }

    private void showDeleteDialog(ActionMode actionMode){
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_files)
                .setMessage(R.string.are_you_sure_delete_files)
                .setPositiveButton(R.string.sure, (dialog, which) -> {
                    deleteSelectedItems();
                    dialog.cancel();
                    actionMode.finish();
                })
                .setNegativeButton(R.string.cancel_dialog, (dialog, which) -> {
                    dialog.cancel();
                    actionMode.finish();
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void shareSelectedItems() {
        List<Integer> selectList = mFileAdapter.getSelectedItems();
        if (selectList != null && !selectList.isEmpty()) {
            MenuItem menuItem = mActionMode.getMenu().findItem(R.id.action_share);
            if (selectList.size() == 1) {
                menuItem.setVisible(true);
                Intent intent = OpenFileUtil.openFile(mFileAdapter.getItem(selectList.get(0)).getPath());
                startActivity(Intent.createChooser(intent, getTitle()));
            } else {
                menuItem.setVisible(false);
            }
        }
    }

    @Override
    public void onRowLongClick(int index) {
        enableActionMode(index);
    }

    @Override
    public void onRowClick(int index) {
        if (mFileAdapter.getSelectedItemCount() > 0) {
            enableActionMode(index);
            return;
        }
        Doc doc = mFileAdapter.getItem(index);
        Intent intent = OpenFileUtil.openFile(doc.getPath());
        if (intent != null) {
            ComponentName componentName = intent.resolveActivity(getPackageManager());
            if (componentName != null) {
                startActivity(Intent.createChooser(intent,getString(R.string.choose_open_file_type)));
            } else {
                Toast.makeText(this, R.string.no_app_for_open_this_file, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.no_app_for_open_this_file, Toast.LENGTH_SHORT).show();
        }

    }

    private class ActionModelCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);
            MainActivity.this.mRefresh.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                MainActivity.this.showDeleteDialog(mode);
            } else if (item.getItemId() == R.id.action_share) {
//                MainActivity.this.shareSelectedItems();
            }
//            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            MainActivity.this.mRefresh.setEnabled(true);
            MainActivity.this.mFileAdapter.clearSelections();
            mActionMode = null;
        }
    }

    private void setupData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermission();
        } else {
            loadData(true);
        }
    }

    /*
    * 从 SDCard 中的 /WIFITransfer/ 中读取文件
    * */
    private void loadData(boolean refresh) {

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String dir = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator
                    + getString(R.string.package_name)
                    + File.separator;

            if (refresh) {
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
                        if (!files.isEmpty()) {
                            Doc doc;
                            for (File f : files) {
                                doc = new Doc();
                                doc.setName(f.getName());
                                doc.setPath(f.getAbsolutePath());
                                doc.setSize(Utils.getFileSizeString(f.length()));
                                long modified = f.lastModified();
                                doc.setModified(modified);
                                doc.setDate(Utils.formatDate(modified));
                                String type = FileTypeUtils.getFileType(f);
                                if (!TextUtils.isEmpty(type) && type.equals("zip") && doc.getName().endsWith("apk")) {
                                    doc.setType("apk");
                                } else if (!TextUtils.isEmpty(type) && type.equals("zip") && doc.getName().endsWith("jar")) {
                                    doc.setType("jar");
                                } else {
                                    doc.setType(type);
                                }
                                docList.add(doc);
                            }
                        }
                        if (!docList.isEmpty()) {
                            Collections.sort(docList, (o1, o2) -> {
                                long docTime1 = o1.getModified();
                                long docTime2 = o2.getModified();
                                if (docTime1 < docTime2) {
                                    return 1;
                                } else if (docTime1 > docTime2) {
                                    return -1;
                                }
                                return 0;
                            });
                        }
                        return docList;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(files -> {
                        if (refresh && mRefresh.isRefreshing()) {
                            mRefresh.setRefreshing(false);
                        }
                        mFileAdapter.replaceData(true, files);
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.e("----", "accept: " + throwable.getMessage());
                        }
                    }));
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkAndRequestPermission() {
        List<String> lackedPermission = new ArrayList<String>();
        if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            lackedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (lackedPermission.isEmpty()) {
            loadData(true);
        } else {
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
            loadData(true);
        } else {
            // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
            Toast.makeText(this, R.string.show_user_go_to_settings, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse(getString(R.string.package_text) + getPackageName()));
            startActivity(intent);
            finish();
        }
    }

    private void initViews() {
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
        mActionModelCallback = new ActionModelCallback();
    }

    private void setupTitle() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }
    }

    private void setupRefresh() {
        mRefresh.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);
        mRefresh.setOnRefreshListener(this);
    }

    private void setupRecycler() {
        mFileAdapter = new FileAdapter(new ArrayList<>(0));
        mFileAdapter.setListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mRecycler.getContext());
        mRecycler.setLayoutManager(layoutManager);
        DividerItemDecoration decoration = new DividerItemDecoration(mRecycler.getContext(), layoutManager.getOrientation());
        decoration.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        mRecycler.addItemDecoration(decoration);
        mRecycler.setHasFixedSize(true);
        mRecycler.setAdapter(mFileAdapter);
    }

    @Override
    public void onRefresh() {
        loadData(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - mCurTimeMills > 2000) {
                Toast.makeText(this, R.string.click_again_to_exit, Toast.LENGTH_SHORT).show();
                mCurTimeMills = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
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

    @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(Constants.RxBusEventType.LOAD_BOOK_LIST)})
    public void loadAppList(Integer type) {
        loadData(true);
    }
}
