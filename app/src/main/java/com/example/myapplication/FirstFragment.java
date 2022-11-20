package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.databinding.FragmentFirstBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FirstFragment extends Fragment {
    private static final String TAG = "FirstFragment";
    private FragmentFirstBinding binding;
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private AudioTrack audioTrack;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handlerThread = new HandlerThread("record");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPlaying) {
                    isPlaying = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                InputStream in = new FileInputStream(new File(getContext().getCacheDir(), "recordCache"));
                                try {
                                    ByteArrayOutputStream out = new ByteArrayOutputStream(
                                            in.available());
                                    for (int b; (b = in.read()) != -1; ) {
                                        out.write(b);
                                    }
                                    Log.d(TAG, "Got the data");
                                    byte[] audioData = out.toByteArray();
                                    createAudioTrack(audioData);
                                } finally {
                                    in.close();
                                }
                            } catch (IOException e) {
                                Log.wtf(TAG, "Failed to read", e);
                            }
                        }
                    });
                } else {
                    isPlaying = false;
                    synchronized (audioTrack) {
                        if (audioTrack != null) {
                            Log.d(TAG, "Stopping");
                            audioTrack.stop();
                            Log.d(TAG, "Releasing");
                            audioTrack.release();
                            Log.d(TAG, "Nulling");
                        }
                    }
                }
            }
        });

        binding.buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioRecord == null) {
                    createAudioRecord();
                }
                if (!isRecording) {
                    audioRecord.startRecording();
                    isRecording = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            FileOutputStream os = null;
                            int read;
                            byte data[] = new byte[recordBufSize];

                            try {
                                String filename = new File(getContext().getCacheDir(), "recordCache").getPath();
                                os = new FileOutputStream(filename);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }

                            if (null != os) {
                                while (isRecording) {
                                    synchronized (audioRecord) {
                                        if (audioRecord != null) {
                                            read = audioRecord.read(data, 0, recordBufSize);

                                            // 如果读取音频数据没有出现错误，就将数据写入到文件
                                            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                                try {
                                                    os.write(data);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }

                                try {
                                    os.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    synchronized (audioRecord) {
                        isRecording = false;
                        audioRecord.stop();
                        audioRecord.release();
                        audioRecord = null;
                    }
                }
            }
        });
    }

    public void createAudioRecord() {
        int frequency = 44100;
        int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        recordBufSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);  //audioRecord能接受的最小的buffer大小
        if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT, recordBufSize);
    }

    public void createAudioTrack(byte[] audioData) {
        this.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                audioData.length, AudioTrack.MODE_STATIC);
        this.audioTrack.write(audioData, 0, audioData.length);
        audioTrack.play();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}