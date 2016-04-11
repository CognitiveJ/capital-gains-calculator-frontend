$(function() {

    $('*[data-hidden]').each(function() {

        var $self = $(this);
        var $hidden = $('#hidden')
        var $input = $self.find('input');

        $input.each(function() {

            var $this = $(this);

            if ($this.val() === 'Yes' && $this.prop('checked')) {
                $hidden.show();
            }
            else {
                $hidden.hide();
            }
        });

        $input.change(function() {

            var $this = $(this);

            if ($this.val() === 'Yes') {
                $hidden.show();
            } else {
                $hidden.hide();
            }
        });
    });``
});