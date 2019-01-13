package org.hotteam67.firebaseviewer.data;

import android.text.TextUtils;

import com.evrencoskun.tableview.ITableView;
import com.evrencoskun.tableview.filter.Filter;
import com.evrencoskun.tableview.filter.FilterItem;
import com.evrencoskun.tableview.filter.FilterType;
import org.hotteam67.firebaseviewer.tableview.MultiFilterTableView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An extension of the filter class that overrides the functions with total rework of the source,
 * using multiple filters instead of just one. ex. adding a team 67 filter doesnt remove team 33,
 * and instead allows both to pass
 */

public class MultiFilter extends Filter {
    private final List<FilterItem> filterItems = new ArrayList<>();
    private final ITableView tableView;

    public MultiFilter(ITableView view)
    {
        super(view);
        tableView = view;
    }

    /**
     * Modified to return the new filter items
     * @return filter items to allow through
     */
    @Override
    public List<FilterItem> getFilterItems()
    {
        return filterItems;
    }


    /**
     * Modified to no longer update existing filters for a column, just adds more for it
     * @param column the column to add a filter to
     * @param filter the string value to allow to pass
     * @param doContains whether to use .contains() or .equals(), .contains() if true
     */
    public void set(int column, String filter, boolean doContains) {
        final FilterItem filterItem = new FilterItem(
                column == -1 ? FilterType.ALL : FilterType.COLUMN,
                column,
                filter
        );

        if (isAlreadyFiltering(column, filterItem)) {
            if (!TextUtils.isEmpty(filter))
                add(filterItem);

        } else if (!TextUtils.isEmpty(filter)) {
            add(filterItem);
        }
        if (tableView instanceof MultiFilterTableView)
            ((MultiFilterTableView) tableView).filter(this, doContains);
        else
            tableView.filter(this);
    }

    /**
     * Adds new filter item to the list of this class.
     * @param filterItem The filter item to be added to the list.
     */
    private void add(FilterItem filterItem) {
        filterItems.add(filterItem);
    }

    public void removeFilter(int column)
    {
        if (filterItems.size() == 0) return;
        for (Iterator<FilterItem> filterItemIterator = filterItems.iterator(); filterItemIterator.hasNext();)
        {
            final FilterItem item = filterItemIterator.next();
            if (column == item.getColumn()) filterItemIterator.remove();
        }
        tableView.filter(this);
    }

    /**
     * Method to check if a filter item is already added based on the column to be filtered.
     *
     * @param column     The column to be checked if the list is already filtering.
     * @param filterItem The filter item to be checked.
     * @return True if a filter item for a specific column or for ALL is already in the list.
     */
    private boolean isAlreadyFiltering(int column, FilterItem filterItem) {
        // This would determine if Filter is already filtering ALL or a specified COLUMN.
        for (FilterItem item : filterItems) {
            if (column == -1 && item.getFilterType().equals(filterItem.getFilterType())) {
                return true;
            } else if (item.getColumn() == filterItem.getColumn()) {
                return true;
            }
        }
        return false;
    }
}
