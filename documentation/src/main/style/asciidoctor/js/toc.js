var versions = [
    // new versions will be dynamically populated by web scraping directory page
    '6.6', '6.5', '6.4', '6.3', '6.2', '6.1', '6.0', // 6.x series
    '5.6', '5.5', '5.4', '5.3', '5.2', '5.1', '5.0', // 5.x series
    '4.3', '4.2', '4.1', '4.0',                      // 4.x series
    '3.6', '3.5', '3.4', '3.3', '3.2'                // 3.x series
];

var directoryUrl = '/hibernate/orm/';

function getVersionOptionHtml(versionText) {
    var linkSuffix;
    if ($.isNumeric(versionText) && versionText < 4.0) {
        linkSuffix = '/reference/en-US/html_single/';
    } else if ($.isNumeric(versionText) && versionText < 5.0) {
        linkSuffix = '/manual/en-US/html_single/';
    } else {
        linkSuffix = '/userguide/html_single/Hibernate_User_Guide.html';
    }
    var link = directoryUrl + versionText + linkSuffix;
    return '<option value="' + link + '" >' + versionText + '</option>';
}

function renderVersions() {
    $('#vchooser').empty();
    $('#vchooser').append('<option>Choose version</option>');
    $('#vchooser').append(getVersionOptionHtml('current'));
    $.each(versions, function(index, version) {
        $('#vchooser').append(getVersionOptionHtml(version));
    });
}

$(document).ready(function() {
    $('#toctitle').before('<select id="vchooser"></select>');

    renderVersions();

    var newVersionFound = false;
    $.ajax({
        url: directoryUrl,
        success: function(html) {
            var anchors = $(html).find('a');
            $.each(anchors, function(index, anchor) {
                var anchorText = anchor.text;
	            if (anchorText.endsWith('/')) {
                    var text = anchorText.substring(0, anchorText.length - 1);
                    if ($.isNumeric(text) && $.inArray(text, versions) < 0) {
                        newVersionFound = true;
                        versions.push(text);
                    }
                }
            });
        },
        error: function() {
            console.log('failed to download directory page: ' + directoryUrl);
        },
        complete: function() {
            if (newVersionFound) {
                versions.sort(function(a, b) { return a == b ? 0 : (a > b ? -1 : 1); });
                renderVersions();
            }
        }
    });

    $('#vchooser').change(function(e) {
        if (this.value !== '')
            window.location.href = this.value;
    });

    $('ul.sectlevel1').wrap('<div id="toctree"></div>');

    $('#toctree').jstree({
         "core" : {
             "themes" : {"variant" : "small", "icons" : false}
         },
         "plugins" : [ "search", "state", "wholerow" ]
    })
    .on("activate_node.jstree", function (e, data) { location.href = data.node.a_attr.href; });

    $('#toctree').before('<input placeholder="&#xf002; Search" id="tocsearch" type="text">');
    var searchTimeout = false;
    $('#tocsearch').keyup(function () {
        if(searchTimeout) { clearTimeout(searchTimeout); }
        searchTimeout = setTimeout(function () {
            var v = $('#tocsearch').val();
            $('#toctree').jstree(true).search(v);
        }, 250);
    });
    $('#tocsearch').after('<a href="#" id="toctreeexpand" title="Expand"><i class="fa fa-plus-square" aria-hidden="true"></i></a><a href="#" id="toctreecollapse" title="Collapse"><i class="fa fa-minus-square" aria-hidden="true"></i></a>');
    $('#toctreeexpand').click(function() { $('#toctree').jstree('open_all'); });
    $('#toctreecollapse').click(function() { $('#toctree').jstree('close_all'); });
});
