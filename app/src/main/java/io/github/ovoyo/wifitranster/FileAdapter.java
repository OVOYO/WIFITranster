package io.github.ovoyo.wifitranster;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    private List<Doc> mDocList;

    FileAdapter(List<Doc> docList) {
        mDocList = docList;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item_layout,parent,false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        Doc doc = mDocList.get(position);
        holder.name.setText(doc.getName());
        holder.size.setText(doc.getSize());
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

        @BindView(R.id.tv_name)
        TextView name;

        @BindView(R.id.tv_size)
        TextView size;

        VH(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
