const snapshotControl = () => {
    return {
        saveCanvas(blob) {
            let reader = new FileReader()
            reader.readAsDataURL(blob)

            reader.onload = () => {
                this.snapshot.data = reader.result

                const done = () => {
                    $('.modal.open').modal('close')
                    M.toast({html: `Snapshot created!`})
                    this.snapshot.name = ''
                    document.dispatchEvent(new CustomEvent('snapshot-event', {}))
                }
                const fail = (resp) => {
                    const errors = resp.responseJSON
                    if (resp.status === 422 && Array.isArray(errors)) {
                        $('#saveSnapshotModal input').each((idx, item) => {
                            const failed = errors.some(e => e.hasOwnProperty(item.name))
                            $(item).toggleClass('invalid', failed).toggleClass('valid', !failed)
                        })
                        errors.forEach(e => M.toast({html: `${Object.keys(e)}: ${Object.values(e)}`}))
                    } else {
                        M.toast({html: `Error ${resp.status}: ${resp.statusText}`})
                    }
                }

                $.post('/private/snapshots/save', JSON.stringify(this.snapshot)).done(done).fail(fail)
            }
        },

        save() {
            const canvas = document.getElementById('wb-canvas')
            canvas.toBlob(this.saveCanvas.bind(this), 'image/png')
        },

        snapshot: {
            sourceBoardId: $('.whiteboard').data('id'),
            name: '',
            data: ''
        },

        list: [],

        loadList() {
            const boardId = $('.whiteboard').data('id')
            $.getJSON(`/private/snapshots/list/${boardId}`).then(data => {
                this.list.length = 0
                this.list.push(...data)
            })
        }
    }
}

const snapshotImage = () => {
    return {
        draw() {
            const ctx = document.getElementById('wb-canvas').getContext('2d')
            ctx.drawImage(this.$el, 0, 0)
        }
    }
}
