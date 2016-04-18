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
            else if($this.val() === 'No' && $this.prop('checked')){
                $hidden.hide();
            }
        });

        $input.change(function() {

            var $this = $(this);

            if ($this.val() === 'Yes') {
                $hidden.show();
            } else if($this.val() === 'No') {
                $hidden.hide();
            }
        });
    });``
});