package max.music_cyclon;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;



public class ControlWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        CharSequence widgetText = context.getString(R.string.control_button);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_widget);
        views.setTextViewText(R.id.control_button, widgetText);

        Intent intent = new Intent(context, LibraryService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.control_button, pendingIntent);


        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}

