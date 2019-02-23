package com.bignerdranch.android.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String FILE_PATH = "file_path";
    private static final String TAG = "errLog";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 3;

    private Crime mCrime;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckBox;
    private Button mDeleteCrime;
    private Intent mData;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private ImageView mPhotoView;
    private File mPhotoFile;

    /*
    Создание фрагмента
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
        Log.e(TAG, "mPhotoFile.getName(): " + mPhotoFile.getName());
    }

    /*
    Настройка представления фрагмента
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Второй параметр определяет родителя представления, третий - указывает, нужно ли включать заполненное представление в родителя
        View v = inflater.inflate(R.layout.fragment_crime, container, false);
        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckBox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        //Удаление элемента
        mDeleteCrime = (Button) v.findViewById(R.id.crime_delete);
        mDeleteCrime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrimeLab.get(getActivity()).deleteCrime(mCrime);
                getActivity().finish();
            }
        });

        mReportButton = (Button) v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));
                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button) v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if(canTakePhoto) {
            Uri uri = FileProvider
                    .getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            //pass permission to the camera
            captureImage.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepInvokeCamera();
                Log.e(TAG, "Запускается активность");
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });
        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        updatePhotoView();
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ImageActivity.class);
                intent.putExtra(FILE_PATH, mPhotoFile.getPath());
                startActivity(intent);
            }
        });

        return v; //Возращается активности-хосту
    }

    //Проверка наличия прав на использование камеры
    private void prepInvokeCamera() {
        //permission check
        if (PermissionChecker.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED){
            return;
        } else{
            String[] permissionRequest = {Manifest.permission.CAMERA};
            requestPermissions(permissionRequest, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            //we are hearing back from the camera
            if(grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                return;
            } else {
                Toast.makeText(getActivity(), getString(R.string.no_camera_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_DATE:
                Date date = (Date) data
                        .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
                mCrime.setDate(date);
                updateDate();
                return;
            case REQUEST_CONTACT:
                if (data == null) return;
                Uri contactUri = data.getData();

                //Определения полей, значения которых должны быть
                //Возвращены запросом
                String[] queryFields = new String[]{
                        ContactsContract.Contacts.DISPLAY_NAME
                };
                //Выполнение запроса - contactUri здесь выполняет функции
                //условия "where"
                Cursor c = getActivity().getContentResolver().query(contactUri, queryFields, null, null, null);
                try {
                    //Проверка получения результатов
                    if (c.getCount() == 0) {
                        return;
                    }
                    //Извлечение первого столбца данных - имени подозреваемого.
                    c.moveToFirst();
                    String suspect = c.getString(0);
                    mCrime.setSuspect(suspect);
                    mSuspectButton.setText(suspect);
                } finally {
                    c.close();
                }
            case REQUEST_PHOTO:
                updatePhotoView();
                return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        CrimeLab.get(getActivity()).UpdateCrime(mCrime);
    }

    private void updateDate() {
        String dateFormat = "dd MMM, EEE";
        String dateString = android.text.format.DateFormat.format(dateFormat, mCrime.getDate()).toString();
        mDateButton.setText(dateString);
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }

        String dateFormat = "dd MMM, EEE";
        String dateString = android.text.format.DateFormat.format(dateFormat, mCrime.getDate()).toString();

        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        String report = getString(R.string.crime_report, mCrime.getTitle(), dateString, solvedString, suspect);

        return report;
    }

    private void updatePhotoView() {
        if(mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }
}
