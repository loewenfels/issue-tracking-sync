var sync = {
    init: function () {
        sync._loadDefinedTrackingApplications();
        sync.readSystemName();
        sync.readInfo();
        sync.updateStatistics();
        setInterval(sync.updateStatistics, 10000);
    },

    readSystemName: function () {
        $.get('./systeminfo')
            .done(function (response) {
                if (response["title"] && response["title"] !== "") {
                    var title = $('.sync-system-name');
                    title.empty();
                    title.append(response["title"]);
                }
                if (response["debug"]) {
                    $('#debug-banner').show()
                } else {
                    $('#debug-banner').hide()
                }
            });
    },
    triggerPolling: function () {
        $.get('./triggerPolling')
            .done(function (response) {
                $('#manual-trigger-error').text("").hide();
                $('#manual-trigger-status').text(response).show()
                    .delay(10000).fadeOut(1000);
            })
            .error(function (error) {
                $('#manual-trigger-status').text("").hide();
                $('#manual-trigger-error').text("An error occurred: " + error).show()
                    .delay(10000).fadeOut(1000);
            });
    },
    readInfo: function () {
        $.get('./info')
            .done(function (response) {
                var container = $('.sync-polling');
                container.empty();
                Object.keys(response).forEach(function (key) {
                    var row = $("<div class='row'/>");
                    var labelCell = $("<div class='col-sm-6'>" + key + "</div>");
                    var valueCell = $("<div class='col-sm-6'>" + response[key] + "</div>");
                    row.append(labelCell).append(valueCell);
                    container.append(row);
                });
            });
    },
    updateStatistics: function () {
        $.get('./statistics')
            .done(function (response) {
                var container = $('.sync-statistics');
                container.empty();
                Object.keys(response).forEach(function (key) {
                    var row = $("<div class='row'/>");
                    var labelCell = $("<div class='col-sm-6'>" + key + "</div>");
                    var value = key.indexOf("EnqueueTime") >= 0 ? response[key] + " ms" : response[key];
                    var valueCell = $("<div class='col-sm-6'>" + value + "</div>");
                    row.append(labelCell).append(valueCell)
                    container.append(row);
                });
            });
    },
    runManual: function () {
        var frm = $('form#manual-sync-form');
        var requestBody = {
            "issuekey": $("[name=issuekey]", frm).val()
        }
        $.ajax({
            url: './manualsync',
            type: 'PUT',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify(requestBody),
            success: function (data) {
                $('#manual-sync-error').text("").hide();
                $('#manual-sync-status').text(data.message).show();
                sync.updateStatistics();
                $('#manual-sync-status').delay(10000).fadeOut(1000);
            },
            error: function (data) {
                $('#manual-sync-status').text("").hide();
                $('#manual-sync-error').text("An error occurred: " + data.message).show()
                    .delay(10000).fadeOut(1000);
            }
        });
    },
    updateSyncIssuesUpdatedAfter: function (date, time) {
        var requestBody = {
            "date": date,
            "time": time ? time : "00:00"
        }
        $.ajax({
            url: './earliestSyncDate',
            type: 'PUT',
            contentType: 'application/json',
            dataType: 'json',
            data: JSON.stringify(requestBody),
            success: function (data) {
                $('#sync-date-error').text("").hide();
                $('#sync-date-status').text(data.message).show()
                    .delay(10000).fadeOut(1000);
                sync.readInfo()
            },
            error: function (data) {
                $('#sync-date-error').text("An error occurred: " + data.message).show()
                    .delay(10000).fadeOut(1000);
                $('#sync-date-status').text("").hide();
                sync.readInfo()
            }
        });
    },
    _loadDefinedTrackingApplications: function () {
        $.get('./definedSystems')
            .done(function (response) {
                var selectControl = $('select#trackingsystem');
                selectControl.empty();
                if (response && response.length > 0) {
                    response.forEach(function (appName) {
                        var opt = $("<option value='" + appName + "'>" + appName + "</option>");
                        selectControl.append(opt);
                    });
                }
            });
    }
};

$(document).ready(function () {
    sync.init();
});
