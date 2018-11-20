package br.pucminas.castro.boxbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsFragment extends Fragment {


    RadioButton combination, noCombination;
    SeekBar cannySoft;
    SeekBar cannyStrong;
    TextView cannySoftText;
    TextView cannyStrongText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_fragment, container, false);
        combination = v.findViewById(R.id.combinationHeuristic);
        noCombination = v.findViewById(R.id.noCombinationHeuristic);
        cannySoft = v.findViewById(R.id.cannyLowBordersSettings);
        cannyStrong = v.findViewById(R.id.cannyStrongBordersSettings);
        cannySoftText = v.findViewById(R.id.actualCannyLowSettings);
        cannyStrongText = v.findViewById(R.id.actualCannyStrongSettings);


        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        int cannySoftPreviousPref = sharedPreferences.getInt("cannySoft", 50);
        int cannyStrongPreviousPref = sharedPreferences.getInt("cannyStrong", 100);
        boolean combinationHeuristic = sharedPreferences.getBoolean("combinationHeuristic", false);

        if(combinationHeuristic){
            combination.setChecked(true);
        }else{
            noCombination.setChecked(true);
        }

        cannySoft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cannySoftText.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        cannyStrong.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cannyStrongText.setText(progress+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        cannySoft.setProgress(cannySoftPreviousPref);
        cannyStrong.setProgress(cannyStrongPreviousPref);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Settings");
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("BoxBoxPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putInt("cannySoft", cannySoft.getProgress());
        sharedPreferencesEditor.putInt("cannyStrong", cannyStrong.getProgress());
        sharedPreferencesEditor.putBoolean("combinationHeuristic", combination.isChecked());
        sharedPreferencesEditor.commit();
        Toast.makeText(getActivity(), "Novas configuracoes Armazenadas", Toast.LENGTH_SHORT).show();
    }
}
