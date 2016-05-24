package lt.marius.travelapse.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import lt.marius.travelapse.R;

public class UIUtils {
	
	private static final Handler uiHandler = new Handler(Looper.getMainLooper());
	
	public static void runOnUiThread(Runnable r) {
		uiHandler.post(r);
	}
	
	public static AlertDialog showOkDialog(Activity context, String title, String message) {
		return showOkDialog(context, title, message, null);
	}

	public static AlertDialog showOkDialog(Activity context, String title, String message, final DialogInterface.OnDismissListener afterDismiss) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(message).setCancelable(false);
			if (title != null) {
				builder.setTitle(title);
			}
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
					if (afterDismiss != null) {
						afterDismiss.onDismiss(dialog);
					}
				}
			});
			if (!context.isFinishing()) {
				AlertDialog alert = builder.create();
				if (title == null) {
					alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
				}
				alert.show();
				try {
					((TextView)alert.findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
				} catch (Exception ex) {}
				return alert;
			}
		} catch (Exception e) {
			e.printStackTrace();
			//ignore
		}
		return null;
	}

	static class FilesAdapter extends BaseAdapter {

		File[] files;

		FilesAdapter(File baseDir, FileFilter filter) {
			files = baseDir.listFiles(filter);
			Arrays.sort(files);
		}

		@Override
		public int getCount() {
			return files.length;
		}

		@Override
		public File getItem(int position) {
			return files[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_files, parent, false);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.text_file_name);
            tv.setText(getItem(position).getName());
			return convertView;
		}
	}

    public interface FileDialogListener {
        void onSelected(File file);
    }

	public static AlertDialog showFilesDialog(Activity context, File baseDir, FileFilter filter, final FileDialogListener listener) {
		try {
			String title = "Select a file";
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setCancelable(true);
			if (title != null) builder.setTitle(title);
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            ViewGroup view = (ViewGroup)LayoutInflater.from(context).inflate(R.layout.dialog_files, null);
			ListView list = (ListView)view.findViewById(R.id.list_view);
			list.setAdapter(new FilesAdapter(baseDir, filter));

            view.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UIUtils.dpToPx(480, context)));
			builder.setView(view);
			if (!context.isFinishing()) {
				final AlertDialog alert = builder.create();
				if (title == null) {
					alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
				}

                list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        File file = (File)parent.getAdapter().getItem(position);
                        alert.dismiss();
                        if (listener != null) {
                            listener.onSelected(file);
                        }
                    }
                });

				alert.show();

				return alert;
			}
		} catch (Exception e) {
			e.printStackTrace();
			//ignore
		}
		return null;
	}
	
//	private float getDip(float pix) {
//
//		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pix, getResources().getDisplayMetrics());
//		return px;
//	}
	
	/**
	 * Returns an array of localized weekday names starting from Monday. 
	 * @param locale - Represents needed language and area codes. E.g. new Locale('fr')
	 * 				   passing null uses default system locale
	 * @return	an array containing translated week days' names
	 */
	public static String[] getLocalizedWeekdays(Locale locale, boolean shortStr) {
		DateFormatSymbols dfSymbols;
		if (locale != null) {
			dfSymbols = new DateFormatSymbols(locale);
		} else {
			dfSymbols = new DateFormatSymbols();
		}
		String[] wDays = shortStr ? dfSymbols.getShortWeekdays() : dfSymbols.getWeekdays();
		int[] days = {Calendar.MONDAY, Calendar.TUESDAY,	//order of days to appear in final array
				      Calendar.WEDNESDAY, Calendar.THURSDAY,
				      Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
		String[] weekDays = new String[days.length];
		for (int i = 0; i < days.length; i++) {	//map results
			weekDays[i] = wDays[days[i]];
		}
		return weekDays;
	}
	
	/**
	 * Returns an array of localized months' names starting from January.
	 * @param languageCode - Represents needed language and area codes. E.g. new Locale('fr')
	 * 						 passing null uses default system locale
	 * @return	an array containing translated months' names
	 */
	public static String[] getLocalizedMonths(Locale locale, boolean shortStr) {
		DateFormatSymbols dfSymbols;
		if (locale != null) {
			dfSymbols = new DateFormatSymbols(locale);
		} else {
			dfSymbols = new DateFormatSymbols();
		}
		String[] allMonths = shortStr ? dfSymbols.getShortMonths() : dfSymbols.getMonths();
		int[] months = {Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH,
						Calendar.APRIL, Calendar.MAY, Calendar.JUNE,
						Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER,
						Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER};
		String[] retMonths = new String[months.length];
		for (int i = 0; i < months.length; i++) {
			retMonths[i] = allMonths[months[i]];
		}
		return retMonths;
	}
	
	public static Bitmap makeLetterBitmap(Bitmap background, String letter) {
		int h = background.getHeight();
		int w = background.getWidth();
		Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setStyle(Paint.Style.FILL_AND_STROKE);
		p.setTypeface(Typeface.DEFAULT_BOLD);
		float heightRatio = 0.8f;
		float textHeight = (float)(h * heightRatio);
		p.setTextSize(textHeight);
		float textWidth = p.measureText(letter);
		while (textWidth > w - 10) {
			heightRatio -= 0.1;
			textHeight = (float)(h * heightRatio);
			p.setTextSize(textHeight);
			textWidth = p.measureText(letter);
		}
		Canvas c = new Canvas();
		c.setBitmap(bmp);
		c.drawBitmap(background, 0, 0, null);
		p.setColor(Color.DKGRAY);
		c.drawText(letter, (w - textWidth) / 2 + 2, h - ((h - textHeight) / 2) - 2, p);
		p.setColor(Color.WHITE);
		c.drawText(letter, (w - textWidth) / 2, h - ((h - textHeight) / 2), p);
		return bmp;
	}

	public static String saveBitmap(Bitmap bmp, String path, String suggestedName) throws IOException {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		//maybe random enough
		if (suggestedName == null) {
//			suggestedName = GeneralUtils.encodeToMD5(new Date().toString());
		}
		File image = new File(dir, suggestedName);
		FileOutputStream out = new FileOutputStream(image);
		bmp.compress(CompressFormat.PNG, 90, out);
		out.flush();
		out.close();
		return image.getAbsolutePath();
	}
	
	public static int dpToPx(float dp, Context c) {
		return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics());
	}

	public static int getRecursiveTop(View anchor) {
		int top = anchor.getTop();
		ViewGroup parent = (ViewGroup)anchor.getParent();
		while (parent.getParent() != null && parent.getParent() instanceof ViewGroup) {
			parent = (ViewGroup)parent.getParent();
			top += parent.getTop();
		}
		return top;
	}
	
	public static int getRecursiveLeft(View anchor) {
		int top = anchor.getLeft();
		ViewGroup parent = (ViewGroup)anchor.getParent();
		while (parent.getParent() != null && parent.getParent() instanceof ViewGroup) {
			parent = (ViewGroup)parent.getParent();
			top += parent.getLeft();
		}
		return top;
	}

	public static void hideSoftKeyboard(Context context, View keyboardTrigger) {
		InputMethodManager imm = (InputMethodManager)context.getSystemService(
			      Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(keyboardTrigger.getWindowToken(),
				0);
	}

	public static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

	public static Bitmap getFlagBitmap(Context c, String shortCode) {
		try {
			return BitmapFactory.decodeStream(c.getAssets().open("flags/" + shortCode.toLowerCase() + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void removeFromParent(View view) {
		if (view != null && view.getParent() != null) {
			((ViewGroup)view.getParent()).removeView(view);
		}
	}

	
	public static void setLocale(String langCode, Context context) {
		Resources res = context.getResources();
	    // Change locale settings in the app.
	    DisplayMetrics dm = res.getDisplayMetrics();
	    android.content.res.Configuration conf = res.getConfiguration();
	    conf.locale = localeFromStr(langCode);
	    res.updateConfiguration(conf, dm);

	}
	
	public static Locale localeFromStr(String str) {
		Locale locale = new Locale("en");
		if (str.length() == 2) {	//e.g. "en"
	    	locale = new Locale(str.toLowerCase());
	    } else if(str.length() == 6) { //e.g. "nl-rBE"
	    	locale = new Locale(str.substring(0, 2), str.substring(4));
	    }
		return locale;
	}
	
//	public static ViewGroup getRootView(View child) {
//		ViewGroup root;
//		if (child instanceof ViewGroup) {
//			root = (ViewGroup) child;
//		} else {
//			root = (ViewGroup) child.getParent();
//		}
//		while (root.getParent() != null && root.getParent() instanceof ViewGroup) {
//			root = (ViewGroup)root.getParent();
//		}
//		return root;
//	}
}
