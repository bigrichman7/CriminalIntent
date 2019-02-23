package com.bignerdranch.android.criminalintent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class CrimeListFragment extends Fragment {

    private static final int REQUEST_CRIME = 1;
    private static final String TAG = "testing";
    private static final String SAVED_SUBTITLE_VISIBLE = "subtible";

    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private int mIndex;
    protected UUID mCrimeId;
    private boolean mSubtitleVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        mCrimeRecyclerView = (RecyclerView) view.findViewById(R.id.crime_recycle_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if(savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        updateUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible );
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        MenuItem subtitleItem = menu.findItem(R.id.menu_item_show_subtitle);
        if(mSubtitleVisible){
            subtitleItem.setTitle(R.string.hide_subtitle);
        } else {
            subtitleItem.setTitle(R.string.show_subtitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new_crime:
                Crime crime = new Crime();
                CrimeLab.get(getActivity()).addCrime(crime);
                Intent intent = CrimePagerActivity
                        .newIntent(getActivity(), crime.getId());
                startActivity(intent);
                return true;
            case R.id.menu_item_show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                getActivity().invalidateOptionsMenu();
                updateSubtitle();
                return true;
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());

        int crimeCount = crimeLab.getCrimes().size();
        @SuppressLint("StringFormatMatches") String subtitle = getString(R.string.subtitle_format, crimeCount);

        if(!mSubtitleVisible){
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity)getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    private void updateUI () {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();
        boolean mChecker = false;

        if(mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else {
            //Поиск позиции списка для обновления данных
            for (int i = 0; i < crimes.size(); i++) {
                if(crimes.get(i).getId().equals(mCrimeId)) {
                    mIndex = i;
                    mAdapter.notifyItemChanged(mIndex);
                    mChecker = true; //true, если данные списка были изменены, но не удалены
                    break;
                }
            }

            //В случае, когда элемент списка был удален
            if(!mChecker) {
                mAdapter.notifyDataSetChanged();
            }
            mAdapter.setCrimes(crimes);
        }
        updateSubtitle();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mTitleTextView;
        private TextView mDateTextView;
        private CheckBox mSolvedCheckBox;

        private Crime mCrime;

        public CrimeHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_crime_title_text_view);
            mDateTextView = (TextView) itemView.findViewById(R.id.list_item_crime_date_text_view);
            mSolvedCheckBox = (CheckBox) itemView.findViewById(R.id.list_item_crime_solved_check_box);
        }

        public void bindCrime(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());

            String dateFormat = "dd MMM, EEE";
            String dateString = android.text.format.DateFormat.format(dateFormat, mCrime.getDate()).toString();
            mDateTextView.setText(dateString);
            mSolvedCheckBox.setChecked(mCrime.isSolved());
        }

        @Override
        public void onClick(View v) {
            mCrimeId = mCrime.getId();
            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            startActivity(intent);
        }
    }


    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {

        private List<Crime> mCrimes;

        public CrimeAdapter (List<Crime> crimes) {
            mCrimes = crimes;
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }

        public void setCrimes(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @Override
        public void onBindViewHolder(@NonNull CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            holder.bindCrime(crime);
        }

        @NonNull
        @Override
        public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);
            return new CrimeHolder(view);
        }
    }
}
