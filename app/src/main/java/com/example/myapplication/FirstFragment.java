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
                                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000,
                                        AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                                        recordBufSize, AudioTrack.MODE_STREAM);
//                                audioTrack.write(audioData, 0, audioData.length);
//                                audioTrack.play();
                                InputStream in = new FileInputStream(new File(getContext().getCacheDir(), "recordCache"));
                                try {
//                                    ByteArrayOutputStream out = new ByteArrayOutputStream(
//                                            in.available());
                                    byte buffer[] = new byte[4096];
                                    synchronized (audioTrack) {
                                        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                                            audioTrack.play();
                                        }
                                        for (int b; (b = in.read(buffer, 0, 4096)) != -1; ) {
                                            audioTrack.write(buffer, 0, b);
                                        }
                                    }
//                                    Log.d(TAG, "Got the data");
//                                    byte[] audioData = out.toByteArray();
//                                    createAudioTrack(audioData);
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (audioRecord == null) {
                            createAudioRecord();
                        }
                    }
                });

                if (!isRecording) {

                    isRecording = true;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            audioRecord.startRecording();
                            FileOutputStream os = null;
                            int read = -3;
                            byte data[] = new byte[recordBufSize * 2];

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
                                            read = audioRecord.read(data, 0, recordBufSize * 2);
                                        } else {
                                            read = -3;
                                        }
                                    }
                                    // 如果读取音频数据没有出现错误，就将数据写入到文件
                                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                                        try {
                                            os.write(data);
                                        } catch (IOException e) {
                                            e.printStackTrace();
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

        binding.buttonReformat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File(getContext().getCacheDir(), "recordCache");
                        if (file.exists()) {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            try {
                                FileInputStream fileInputStream = new FileInputStream(file);
                                byte buffer[] = new byte[4096];
                                int len;
                                while ((len = fileInputStream.read(buffer)) > 0) {
                                    byteArrayOutputStream.write(buffer);
                                }
                                byte[] readBuffer = byteArrayOutputStream.toByteArray();
                                byte[] sendBuffer = new byte[readBuffer.length / 2];
                                for (int i = 0; i < readBuffer.length - 1; i += 2) {
                                    if ((readBuffer[i + 1] & 0x80) == 0x80) {
                                        sendBuffer[i / 2] = (byte) (readBuffer[i + 1] & 0x7f);
                                    } else {
                                        sendBuffer[i / 2] = (byte) (readBuffer[i + 1] + 0x80);
                                    }
                                }
                                FileOutputStream fos = new FileOutputStream(new File(getContext().getCacheDir(), "recordCache1"));
                                fos.write(sendBuffer);
                                fos.flush();
                                fos.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }

    public void createAudioRecord() {
        int frequency = 16000;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
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
        audioRecord = new AudioRecord(7, frequency, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT, recordBufSize * 2);
    }

    public void createAudioTrack(byte[] audioData) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}