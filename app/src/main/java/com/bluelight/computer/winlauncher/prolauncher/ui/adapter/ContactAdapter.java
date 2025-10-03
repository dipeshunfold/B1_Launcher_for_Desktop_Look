package com.bluelight.computer.winlauncher.prolauncher.ui.adapter;

import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluelight.computer.winlauncher.prolauncher.R;
import com.bluelight.computer.winlauncher.prolauncher.model.ContactModel;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final OnContactClickListener listener;
    private List<ContactModel> contactList;

    public ContactAdapter(List<ContactModel> contacts, OnContactClickListener listener) {
        this.contactList = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contactList.get(position);

        holder.name.setText(contact.getName());
        holder.number.setText(contact.getNumber());

        String photoUri = contact.getPhotoUri();
        if (photoUri != null && !photoUri.isEmpty()) {
            holder.photo.setVisibility(View.VISIBLE);
            holder.initial.setVisibility(View.GONE);


            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(photoUri))
                    .override(100, 100)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.photo);
        } else {
            holder.photo.setVisibility(View.GONE);
            holder.initial.setVisibility(View.VISIBLE);

            String name = contact.getName().trim();
            if (!name.isEmpty()) {
                String firstLetter = String.valueOf(name.charAt(0)).toUpperCase();
                holder.initial.setText(firstLetter);

                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);

                int color = ColorGenerator.getColor(firstLetter);
                circle.setColor(color);
                holder.initial.setBackground(circle);

            } else {
                holder.initial.setText("?");
                holder.initial.setBackgroundResource(R.drawable.default_circle_background);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact.getNumber()));
    }

    @Override
    public int getItemCount() {
        return contactList != null ? contactList.size() : 0;
    }

    public void updateList(List<ContactModel> newList) {
        this.contactList = newList;
        notifyDataSetChanged();
    }

    public interface OnContactClickListener {
        void onContactClick(String number);
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name, number, initial;
        CircleImageView photo;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textContactName);
            number = itemView.findViewById(R.id.textContactNumber);
            photo = itemView.findViewById(R.id.imageContactPhoto);
            initial = itemView.findViewById(R.id.textContactInitial);
        }
    }
}