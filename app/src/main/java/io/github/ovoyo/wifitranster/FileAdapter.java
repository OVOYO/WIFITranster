package io.github.ovoyo.wifitranster;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;


public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    private List<Doc> mDocList;

    FileAdapter(List<Doc> docList) {
        mDocList = docList;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.doc_item_layout,parent,false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Doc doc = mDocList.get(position);
        String name = TextUtils.isEmpty(doc.getName()) ? "" : doc.getName();
        String size = TextUtils.isEmpty(doc.getSize()) ? "" : doc.getSize();
        String type = TextUtils.isEmpty(doc.getType()) ? "" : doc.getType();
        String time = TextUtils.isEmpty(doc.getModified()) ? "" : doc.getModified();
        holder.name.setText(name);
        holder.size.setText(size);
        holder.type.setText(type);
        holder.time.setText(time);
        if (TextUtils.isEmpty(type)){
            holder.textIcon.setText("文件");
        }else {
            holder.textIcon.setText(type.toUpperCase(Locale.getDefault()));
        }
    }

    @Override
    public int getItemCount() {
        return mDocList == null ? 0 : mDocList.size();
    }

    public void replaceData(boolean clear, List<Doc> data){
        if (data == null || data.isEmpty()){
            return;
        }
        if (mDocList == null){
            mDocList = new ArrayList<>();
        }
        if (clear && !mDocList.isEmpty()){
            mDocList.clear();
        }
        mDocList.addAll(data);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder{

        @BindView(R.id.doc_icon)
        CircleImageView icon;

        @BindView(R.id.doc_text_icon)
        TextView textIcon;

        @BindView(R.id.doc_name)
        TextView name;

        @BindView(R.id.doc_size)
        TextView size;

        @BindView(R.id.doc_type)
        TextView type;

        @BindView(R.id.doc_time)
        TextView time;

        VH(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
