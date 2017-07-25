package com.duan.musicoco.aidl;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.duan.musicoco.service.PlayController;

import java.util.List;

/**
 * Created by DuanJiaNing on 2017/5/23.<br><br>
 * 仅有从 {@link com.duan.musicoco.aidl.IPlayControl}.aidl 继承的方法在跨进程调用时有效<br>
 * 1. 该类中的方法运行在服务端 Binder 线程池中，所有需要处理线程同步<br>
 * 2. 这些方法被客户端调用时客户端线程会被挂起，如果客户端的线程为 UI 线程，注意处理耗时操作以避免出现的 ANR<br>
 */

public class PlayControlImpl extends com.duan.musicoco.aidl.IPlayControl.Stub {

    protected RemoteCallbackList<IOnSongChangedListener> mSongChangeListeners;

    protected RemoteCallbackList<IOnPlayStatusChangedListener> mStatusChangeListeners;

    protected RemoteCallbackList<IOnPlayListChangedListener> mPlayListChangeListeners;

    protected RemoteCallbackList<IOnDataIsReadyListener> mDataIsReadyListeners;

    private PlayController manager;

    private Context context;

    public PlayControlImpl(Context context) {
        this.context = context;
        this.mSongChangeListeners = new RemoteCallbackList<>();
        this.mStatusChangeListeners = new RemoteCallbackList<>();
        this.mPlayListChangeListeners = new RemoteCallbackList<>();
        this.mDataIsReadyListeners = new RemoteCallbackList<>();
        this.manager = PlayController.getMediaController(
                context,
                new NotifyStatusChange(),
                new NotifySongChange(),
                new NotifyPlayListChange());
    }

    /**
     * 播放相同列表中的指定曲目
     *
     * @param which 曲目
     * @return 播放是否成功
     */
    @Override
    public synchronized int play(Song which) {
        if (which == null)
            return -1;
        int re = PlayController.ERROR_UNKNOWN;
        if (manager.getCurrentSong() != which) {
            re = manager.play(which);
        }
        return re;
    }

    @Override
    public int playByIndex(int index) {
        int re = PlayController.ERROR_UNKNOWN;
        if (manager.getSongsList().get(index) != null && manager.getCurrentSongIndex() != index) {
            re = manager.play(index);
        }
        return re;
    }

    @Override
    public int getAudioSessionId() throws RemoteException {
        return manager.getAudioSessionId();
    }

    @Override
    public int setCurrentSong(Song song) throws RemoteException {
        if (song == null)
            return -1;
        return manager.prepare(song);
    }

    /**
     * 该方法并没有在 aidl 文件中声明，客户端不应调用该方法
     *
     * @param index 播放列表对应下标
     */
    public int play(int index) {
        return manager.play(index);
    }

    @Override
    public synchronized Song pre() {
        Song pre = manager.getCurrentSong();
        Song s = manager.preSong();
        return s;
    }

    @Override
    public synchronized Song next() {
        Song pre = manager.getCurrentSong();
        //随机播放时可能播放同一首
        Song next = manager.nextSong();
        return next;
    }

    @Override
    public synchronized int pause() {
        return manager.pause();
    }

    @Override
    public synchronized int resume() {
        return manager.resume();
    }

    @Override
    public Song currentSong() {
        return manager.getCurrentSong();
    }

    @Override
    public int currentSongIndex() {
        return manager.getCurrentSongIndex();
    }

    @Override
    public int status() {
        return manager.getPlayState();
    }

    @Override
    public Song setPlayList(List<Song> songs, int current, int id) throws RemoteException {

        if (songs.size() <= 0) {
            return null;
        }

        int cu = 0;
        if (current >= 0 && current < songs.size()) {
            cu = current;
        }

        Song n = manager.setPlayList(songs, cu, id);
        return n;
    }

    @Override
    public Song setPlaySheet(int sheetID, int current) throws RemoteException {
        return manager.setPlaySheet(sheetID, current);
    }

    @Override
    public List<Song> getPlayList() {
        return manager.getSongsList();
    }

    @Override
    public int getPlayListId() throws RemoteException {
        return manager.getPlayListId();
    }

    @Override
    public synchronized void setPlayMode(int mode) {
        if (mode >= PlayController.MODE_DEFAULT && mode <= PlayController.MODE_RANDOM)
            manager.setPlayMode(mode);
    }

    @Override
    public int getProgress() {
        return manager.getProgress();
    }

    @Override
    public synchronized int seekTo(int pos) {
        return manager.seekTo(pos);
    }

    @Override
    public void remove(Song song) throws RemoteException {
        manager.remove(song);
    }

    @Override
    public int getPlayMode() throws RemoteException {
        return manager.getPlayMode();
    }

    @Override
    public void registerOnSongChangedListener(IOnSongChangedListener li) {
        mSongChangeListeners.register(li);
    }

    @Override
    public void registerOnPlayStatusChangedListener(IOnPlayStatusChangedListener li) {
        mStatusChangeListeners.register(li);
    }

    @Override
    public void registerOnPlayListChangedListener(IOnPlayListChangedListener li) throws RemoteException {
        mPlayListChangeListeners.register(li);
    }

    @Override
    public void registerOnDataIsReadyListener(IOnDataIsReadyListener li) throws RemoteException {
        mDataIsReadyListeners.register(li);
    }

    @Override
    public void unregisterOnSongChangedListener(IOnSongChangedListener li) {
        mSongChangeListeners.unregister(li);
    }

    @Override
    public void unregisterOnPlayStatusChangedListener(IOnPlayStatusChangedListener li) {
        mStatusChangeListeners.unregister(li);
    }

    @Override
    public void unregisterOnPlayListChangedListener(IOnPlayListChangedListener li) throws RemoteException {
        mPlayListChangeListeners.unregister(li);
    }

    @Override
    public void unregisterOnDataIsReadyListener(IOnDataIsReadyListener li) throws RemoteException {
        mDataIsReadyListeners.unregister(li);
    }

    public void releaseMediaPlayer() {
        manager.releaseMediaPlayer();
    }

    public void notifyDataIsReady() {
        Log.d("service", "notifyDataIsReady: ");
        final int N = mDataIsReadyListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnDataIsReadyListener listener = mDataIsReadyListeners.getBroadcastItem(i);
            if (listener != null) {
                try {
                    listener.dataIsReady();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mDataIsReadyListeners.finishBroadcast();
    }

    private class NotifySongChange implements PlayController.NotifySongChanged {

        @Override
        public void notify(Song song, int index, boolean isNext) {
            Log.d("service", "NotifySongChanged: ");
            final int N = mSongChangeListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IOnSongChangedListener listener = mSongChangeListeners.getBroadcastItem(i);
                if (listener != null) {
                    try {
                        listener.onSongChange(song, index, isNext);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mSongChangeListeners.finishBroadcast();
        }
    }

    private class NotifyStatusChange implements PlayController.NotifyStatusChanged {

        @Override
        public void notify(Song song, int index, int status) {
            Log.d("service", "NotifyStatusChanged: ");
            final int N = mStatusChangeListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IOnPlayStatusChangedListener listener = mStatusChangeListeners.getBroadcastItem(i);
                if (listener != null) {
                    try {
                        switch (status) {
                            case PlayController.STATUS_START:
                                listener.playStart(song, index, status);
                                break;
                            case PlayController.STATUS_STOP:
                                listener.playStop(song, index, status);
                                break;
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mStatusChangeListeners.finishBroadcast();
        }
    }

    private class NotifyPlayListChange implements PlayController.NotifyPlayListChanged {

        @Override
        public void notify(Song current, int index, int id) {

            final int N = mPlayListChangeListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IOnPlayListChangedListener listener = mPlayListChangeListeners.getBroadcastItem(i);
                if (listener != null) {
                    try {
                        listener.onPlayListChange(current, index, id);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mPlayListChangeListeners.finishBroadcast();
        }
    }
}
