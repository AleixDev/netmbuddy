package free.yhc.youtube.musicplayer.model;

import static free.yhc.youtube.musicplayer.model.Utils.eAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;


public class YTDownloader {
    private static final int MSG_WHAT_DOWNLOAD  = 0;

    private String                      mProxy      = null;
    private DownloadDoneReceiver        mDnDoneRcvr = null;
    private BGHandler                   mBgHandler  = null;

    private YTHacker                    mYtHack     = null;

    public interface DownloadDoneReceiver {
        void downloadDone(YTDownloader downloader, DnArg arg, Err err);
    }

    public static class DnArg {
        String  ytvid;
        File    outf;
        int     qscore;
        public DnArg(String aYtvid, File aOutf) {
            ytvid = aYtvid;
            outf = aOutf;
            // default is lowest quality.
            qscore = YTHacker.YTQUALITY_SCORE_LOWEST;
        }

        public DnArg(String aYtvid, File aOutf, int aQscore) {
            ytvid = aYtvid;
            outf = aOutf;
            qscore = aQscore;
        }
    }

    private class BGThread extends HandlerThread {
        BGThread() {
            super("YTDownloader.BGThread",Process.THREAD_PRIORITY_BACKGROUND);
        }
        @Override
        protected void
        onLooperPrepared() {
            super.onLooperPrepared();
        }
    }

    private class BGHandler extends Handler {
        private File    mTmpF = null;
        BGHandler(Looper looper) {
            super(looper);
        }

        private void
        sendResult(final DnArg arg, final Err result) {
            if (null == mDnDoneRcvr)
                return;

            Utils.getUiHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (null != mDnDoneRcvr)
                        mDnDoneRcvr.downloadDone(YTDownloader.this, arg, result);
                }
            });
        }

        // This is synchronous function.
        // That is, ONLY one file can be download in one YTDownloader instance at a time.
        private void
        handleDownload(DnArg arg) {
            if (null != mTmpF)
                mTmpF.delete();

            YTHacker hack = new YTHacker(arg.ytvid);
            NetLoader loader = null;
            try {
                Err result = hack.start();
                if (Err.NO_ERR != result) {
                    sendResult(arg, result);
                    return;
                }
                loader = hack.getNetLoader();
                YTHacker.YtVideo vid = hack.getVideo(arg.qscore);
                eAssert(null != vid);
                NetLoader.HttpRespContent content = loader.getHttpContent(Uri.parse(vid.url), false);
                if (HttpUtils.SC_NO_CONTENT == content.stcode) {
                    sendResult(arg, Err.YTHTTPGET);
                    return;
                }

                // Download to temp file.
                mTmpF = File.createTempFile(arg.ytvid, null, new File(Policy.APPDATA_TMPDIR));
                FileOutputStream fos = new FileOutputStream(mTmpF);
                Utils.copy(fos, content.stream);
                // file returned by YTHacker is mpeg format!
                mTmpF.renameTo(arg.outf);
                sendResult(arg, Err.NO_ERR);
            } catch (FileNotFoundException e) {
                sendResult(arg, Err.IO_FILE);
            } catch (IOException e) {
                sendResult(arg, Err.IO_FILE);
            } catch (YTMPException e) {
                sendResult(arg, e.getError());
            } finally {
                if (null != loader)
                    loader.close();

                if (null != mTmpF)
                    mTmpF.delete();
            }
        }

        @Override
        public void
        handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_WHAT_DOWNLOAD:
                handleDownload((DnArg)msg.obj);
                break;
            }
        }
    }


    public YTDownloader() {
    }

    /**
     *
     * @param ytvid
     *   11-character-long youtube video id
     */
    public Err
    download(final String ytvid, final File outf) {
        eAssert(Utils.isUiThread());

        if (outf.exists()) {
            if (null != mDnDoneRcvr) {
                // already downloaded.
                Utils.getUiHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mDnDoneRcvr.downloadDone(YTDownloader.this,
                                                 new DnArg(ytvid, outf),
                                                 Err.NO_ERR);
                    }
                });
            }
            return Err.NO_ERR;
        }

        if (Utils.isNetworkAvailable()) {
            Message msg = mBgHandler.obtainMessage(MSG_WHAT_DOWNLOAD,
                                                   new DnArg(ytvid, outf));
            mBgHandler.sendMessage(msg);
            return Err.NO_ERR;
        }
        return Err.NETWORK_UNAVAILABLE;
    }

    public void
    open(String proxy, DownloadDoneReceiver dnDoneRcvr) {
        mProxy = proxy;
        mDnDoneRcvr = dnDoneRcvr;

        HandlerThread hThread = new BGThread();
        hThread.start();
        mBgHandler = new BGHandler(hThread.getLooper());
    }

    public void
    close() {
        // TODO
        // Stop running thread!
        // Need to check that below code works as expected perfectly.
        // "interrupting thread" is quite annoying and unpredictable job!
        if (null != mBgHandler) {
            mBgHandler.getLooper().getThread().interrupt();
        }
    }
}
